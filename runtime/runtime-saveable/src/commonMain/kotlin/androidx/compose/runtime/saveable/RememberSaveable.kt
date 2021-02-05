/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive the activity or process
 * recreation using the saved instance state mechanism (for example it happens when the screen is
 * rotated in the Android application).
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveable
 *
 * If you use it with types which can be stored inside the Bundle then it will be saved and
 * restored automatically using [autoSaver], otherwise you will need to provide a custom [Saver]
 * implementation via the [saver] param.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableCustomSaver
 *
 * You can use it with a value stored inside [androidx.compose.runtime.mutableStateOf].
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableState
 *
 * If the value inside the MutableState can be stored inside the Bundle it would be saved
 * and restored automatically, otherwise you will need to provide a custom [Saver]
 * implementation via an overload with which has `stateSaver` param.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableStateAndCustomSaver
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 * reset and [init] to be rerun
 * @param saver The [Saver] object which defines how the state is saved and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 * automatically generated by the Compose runtime which is unique for the every exact code location
 * in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@Composable
fun <T : Any> rememberSaveable(
    vararg inputs: Any?,
    saver: Saver<T, out Any> = autoSaver(),
    key: String? = null,
    init: () -> T
): T {
    // key is the one provided by the user or the one generated by the compose runtime
    val finalKey = if (!key.isNullOrEmpty()) {
        key
    } else {
        currentCompositeKeyHash.toString()
    }
    @Suppress("UNCHECKED_CAST")
    (saver as Saver<T, Any>)

    val registry = LocalSaveableStateRegistry.current
    // value is restored using the registry or created via [init] lambda
    val value = remember(*inputs) {
        // TODO not restore when the input values changed (use hashKeys?) b/152014032
        val restored = registry?.consumeRestored(finalKey)?.let {
            saver.restore(it)
        }
        restored ?: init()
    }

    // save the latest passed saver object into a state object to be able to use it when we will
    // be saving the value. keeping value in mutableStateOf() allows us to properly handle
    // possible compose transactions cancellations
    val saverHolder = remember { mutableStateOf(saver) }
    saverHolder.value = saver

    // re-register if the registry or key has been changed
    if (registry != null) {
        DisposableEffect(registry, finalKey) {
            val valueProvider = {
                with(saverHolder.value) { SaverScope { registry.canBeSaved(it) }.save(value) }
            }
            registry.requireCanBeSaved(valueProvider())
            val entry = registry.registerProvider(finalKey, valueProvider)
            onDispose {
                entry.unregister()
            }
        }
    }
    return value
}

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive the activity or process
 * recreation using the saved instance state mechanism (for example it happens when the screen is
 * rotated in the Android application).
 *
 * Use this overload if you remember a mutable state with a type which can't be stored in the
 * Bundle so you have to provide a custom saver object.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableStateAndCustomSaver
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 * reset and [init] to be rerun
 * @param stateSaver The [Saver] object which defines how the value inside the MutableState is
 * saved and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 * automatically generated by the Compose runtime which is unique for the every exact code location
 * in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@Composable
fun <T> rememberSaveable(
    vararg inputs: Any?,
    stateSaver: Saver<T, out Any>,
    key: String? = null,
    init: () -> MutableState<T>
): MutableState<T> = rememberSaveable(
    inputs,
    saver = mutableStateSaver(stateSaver),
    key = key,
    init = init
)

@Suppress("UNCHECKED_CAST")
private fun <T> mutableStateSaver(inner: Saver<T, out Any>) = with(inner as Saver<T, Any>) {
    Saver<MutableState<T>, MutableState<Any?>>(
        save = { state ->
            require(state is SnapshotMutableState<T>) {
                "If you use a custom MutableState implementation you have to write a custom " +
                    "Saver and pass it as a saver param to rememberSaveable()"
            }
            mutableStateOf(save(state.value), state.policy as SnapshotMutationPolicy<Any?>)
        },
        restore = @Suppress("UNCHECKED_CAST") {
            require(it is SnapshotMutableState<Any?>)
            mutableStateOf(
                if (it.value != null) restore(it.value!!) else null,
                it.policy as SnapshotMutationPolicy<T?>
            ) as MutableState<T>
        }
    )
}

private fun SaveableStateRegistry.requireCanBeSaved(value: Any?) {
    if (value != null && !canBeSaved(value)) {
        throw IllegalArgumentException(
            if (value is SnapshotMutableState<*>) {
                if (value.policy !== neverEqualPolicy<Any?>() &&
                    value.policy !== structuralEqualityPolicy<Any?>() &&
                    value.policy !== referentialEqualityPolicy<Any?>()
                ) {
                    "If you use a custom SnapshotMutationPolicy for your MutableState you have to" +
                        " write a custom Saver"
                } else {
                    "MutableState containing ${value.value} cannot be saved using the current " +
                        "SaveableStateRegistry. The default implementation only supports types " +
                        "which can be stored inside the Bundle. Please consider implementing a " +
                        "custom Saver for this class and pass it as a stateSaver parameter to " +
                        "rememberSaveable()."
                }
            } else {
                "$value cannot be saved using the current SaveableStateRegistry. The default " +
                    "implementation only supports types which can be stored inside the Bundle" +
                    ". Please consider implementing a custom Saver for this class and pass it" +
                    " to rememberSaveable()."
            }
        )
    }
}

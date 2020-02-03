/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.benchmark

import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.Observe
import androidx.compose.benchmark.realworld4.RealWorld4_FancyWidget_000
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.ui.core.draw
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.Container
import androidx.ui.unit.dp
import androidx.ui.unit.toRect
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ComposeBenchmark : ComposeBenchmarkBase() {

    @UiThreadTest
    @Test
    fun benchmark_01_Compose_OneRect() {
        val model = ColorModel()
        measureCompose {
            OneRect(model)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_02_Compose_TenRects() {
        val model = ColorModel()
        measureCompose {
            TenRects(model)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_03_Compose_100Rects() {
        val model = ColorModel()
        measureCompose {
            HundredRects(model = model)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_04_Recompose_OneRect() {
        val model = ColorModel()
        measureRecompose {
            compose {
                OneRect(model)
            }
            update {
                model.toggle()
            }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_05_Recompose_TenRect_Wide() {
        val model = ColorModel()
        measureRecompose {
            compose {
                TenRects(model, narrow = false)
            }
            update {
                model.toggle()
            }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_06_Recompose_TenRect_Narrow() {
        val model = ColorModel()
        measureRecompose {
            compose {
                TenRects(model, narrow = true)
            }
            update {
                model.toggle()
            }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_07_Recompose_100Rect_Wide() {
        val model = ColorModel()
        measureRecompose {
            compose {
                HundredRects(model, narrow = false)
            }
            update {
                model.toggle()
            }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_08_Recompose_100Rect_Narrow() {
        val model = ColorModel()
        measureRecompose {
            compose {
                HundredRects(model, narrow = true)
            }
            update {
                model.toggle()
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore("Disabled as it appears to not do anything")
    fun benchmark_realworld4_mid_recompose() {
        val model = androidx.compose.benchmark.realworld4.createSampleData()
        measureRecompose {
            compose {
                RealWorld4_FancyWidget_000(model)
            }
            update {
                model.f2.f15.f1.f1.f1_modified = !model.f2.f15.f1.f1.f1_modified
            }
        }
    }
}

private fun background(paint: Paint) =
    draw { canvas, size -> canvas.drawRect(size.toRect(), paint) }

private val redBackground = background(Paint().also { it.color = Color.Red })
private val blackBackground = background(Paint().also { it.color = Color.Black })
private val yellowBackground = background(Paint().also { it.color = Color.Yellow })
private val defaultBackground = yellowBackground

private val dp10 = 10.dp

@Model
class ColorModel(private var color: Color = Color.Black) {
    fun toggle() {
        color = if (color == Color.Black) Color.Red else Color.Black
    }

    val background
        get() = when (color) {
            Color.Red -> redBackground
            Color.Black -> blackBackground
            Color.Yellow -> yellowBackground
            else -> background(Paint().also { it.color = color })
        }
}

val noChildren = @Composable { }
@Composable
fun OneRect(model: ColorModel) {
    Container(
        modifier = model.background,
        width = dp10,
        height = dp10,
        expanded = true,
        children = noChildren
    )
}

@Composable
fun TenRects(model: ColorModel, narrow: Boolean = false) {
    if (narrow) {
        Observe {
            Container(
                modifier = model.background,
                width = dp10,
                height = dp10,
                expanded = true,
                children = noChildren
            )
        }
    } else {
        Container(
            modifier = model.background,
            width = dp10,
            height = dp10,
            expanded = true,
            children = noChildren
        )
    }
    repeat(9) {
        Container(
            modifier = defaultBackground,
            width = dp10,
            height = dp10,
            expanded = true,
            children = noChildren
        )
    }
}

@Composable
fun HundredRects(model: ColorModel, narrow: Boolean = false) {
    repeat(100) {
        if (it % 10 == 0)
            if (narrow) {
                Observe {
                    Container(
                        modifier = model.background,
                        width = dp10,
                        height = dp10,
                        expanded = true,
                        children = noChildren
                    )
                }
            } else {
                Container(
                    modifier = model.background,
                    width = dp10,
                    height = dp10,
                    expanded = true,
                    children = noChildren
                )
            }
        else
            Container(
                modifier = defaultBackground,
                width = dp10,
                height = dp10,
                expanded = true,
                children = noChildren
            )
    }
}

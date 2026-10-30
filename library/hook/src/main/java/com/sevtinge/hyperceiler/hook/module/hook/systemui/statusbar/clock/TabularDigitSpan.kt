/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.clock

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import kotlin.math.ceil
import kotlin.math.max

/**
 * 让一个数字字符占用固定宽度（等于当前字体中最宽的数字），
 * 这样秒、日期等位置不会因为 "1" 比 "0" 窄而左右抖动，不依赖字体是否支持等宽数字。
 */
class TabularDigitSpan : ReplacementSpan() {

    private fun cellWidth(paint: Paint): Int {
        var widest = 0f
        for (c in '0'..'9') {
            widest = max(widest, paint.measureText(c.toString()))
        }
        return ceil(widest.toDouble()).toInt()
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        if (fm != null) {
            val metrics = paint.fontMetricsInt
            fm.ascent = metrics.ascent
            fm.descent = metrics.descent
            fm.top = metrics.top
            fm.bottom = metrics.bottom
        }
        return cellWidth(paint)
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        if (text == null) return
        val digit = text.subSequence(start, end).toString()
        val offset = (cellWidth(paint) - paint.measureText(digit)) / 2f
        canvas.drawText(digit, x + offset, y.toFloat(), paint)
    }
}

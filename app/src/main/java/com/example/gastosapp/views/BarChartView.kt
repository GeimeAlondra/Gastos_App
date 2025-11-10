package com.example.gastosapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pintura para las barras
    private val barPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")  // Verde
        style = Paint.Style.FILL
    }

    // Pintura para el texto
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    // Pintura para los ejes
    private val axisPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    // Datos del gráfico: Lista de (etiqueta, valor)
    private var data: List<Pair<String, Float>> = emptyList()
    private var maxValue: Float = 0f

    // Variables para controlar el ancho y espaciado de barras
    private var barWidthFactor: Float = 0.9f
    private var barSpacing: Float = 0.1f // 10% de espacio entre barras

    /**
     * Establece los datos para el gráfico
     * @param newData Lista de pares (etiqueta, valor)
     */
    fun setData(newData: List<Pair<String, Float>>) {
        data = newData
        // Calcular el valor máximo para escalar las barras
        maxValue = if (data.isNotEmpty()) data.maxOf { it.second } * 1.1f else 1f
        // Redibujar el view
        invalidate()
    }

    /**
     * Establece el ancho de las barras (0.1f a 1.0f)
     */
    fun setBarWidthFactor(factor: Float) {
        barWidthFactor = factor.coerceIn(0.1f, 1.0f)
        invalidate()
    }

    /**
     * Establece el espacio entre barras
     * @param spacing Valor entre 0.0f (sin espacio) y 0.5f (50% de espacio)
     */
    fun setBarSpacing(spacing: Float) {
        barSpacing = spacing.coerceIn(0.0f, 0.5f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Si no hay datos, mostrar mensaje
        if (data.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        // Dibujar los elementos del gráfico
        drawBars(canvas)
        drawLabels(canvas)
        drawAxes(canvas)
    }

    /**
     * Dibuja mensaje cuando no hay datos
     */
    private fun drawNoDataMessage(canvas: Canvas) {
        val text = "No hay datos"
        val x = width / 2f
        val y = height / 2f

        textPaint.textSize = 48f
        textPaint.color = Color.GRAY
        canvas.drawText(text, x, y, textPaint)
    }

    /**
     * Dibuja las barras del gráfico CON ESPACIADO
     */
    private fun drawBars(canvas: Canvas) {
        // Espacio total disponible para barras (con márgenes)
        val totalWidthForBars = width - 80f
        val barWidth = totalWidthForBars / data.size.toFloat()

        // Calcular el ancho real de cada barra (considerando el espaciado)
        val actualBarWidth = barWidth * (1f - barSpacing)
        val spacingWidth = barWidth * barSpacing

        val maxBarHeight = height - 120f

        data.forEachIndexed { index, (label, value) ->
            // Calcular altura de la barra proporcional al valor máximo
            val barHeight = (value / maxValue) * maxBarHeight

            // Calcular posición de la barra CON ESPACIADO
            val left = 40f + (index * barWidth) + (spacingWidth / 2)
            val top = height - 80f - barHeight
            val right = left + actualBarWidth * barWidthFactor
            val bottom = height - 80f

            // Dibujar barra
            canvas.drawRect(left, top, right, bottom, barPaint)

            // Dibujar valor encima de la barra (solo si es mayor a 0)
            if (value > 0) {
                textPaint.textSize = 28f
                textPaint.color = Color.BLACK
                canvas.drawText(
                    "$${String.format("%.0f", value)}",
                    (left + right) / 2,
                    top - 15f,
                    textPaint
                )
            }
        }
    }

    /**
     * Dibuja las etiquetas en el eje X (ajustado para el espaciado)
     */
    private fun drawLabels(canvas: Canvas) {
        val totalWidthForBars = width - 80f
        val barWidth = totalWidthForBars / data.size.toFloat()

        data.forEachIndexed { index, (label, value) ->
            // Centrar la etiqueta en el espacio de la barra + espaciado
            val x = 40f + (index * barWidth) + (barWidth / 2)
            val y = height - 40f

            textPaint.textSize = 30f
            textPaint.color = Color.BLACK
            canvas.drawText(label, x, y, textPaint)
        }
    }

    /**
     * Dibuja los ejes X e Y
     */
    private fun drawAxes(canvas: Canvas) {
        // Eje Y (vertical) - más delgado
        canvas.drawLine(40f, height - 80f, 40f, 60f, axisPaint)

        // Eje X (horizontal) - más delgado
        canvas.drawLine(40f, height - 80f, width - 40f, height - 80f, axisPaint)

        // Marca en el máximo valor
        if (maxValue > 0) {
            textPaint.textSize = 24f  // Texto más pequeño
            textPaint.color = Color.GRAY
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$${String.format("%.0f", maxValue)}", 35f, 50f, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
        }
    }

    /**
     * Cambia el color de las barras
     */
    fun setBarColor(color: Int) {
        barPaint.color = color
        invalidate()
    }

    /**
     * Establece el tamaño del texto
     */
    fun setTextSize(size: Float) {
        textPaint.textSize = size
        invalidate()
    }
}
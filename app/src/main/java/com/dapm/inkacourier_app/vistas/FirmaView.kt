package com.dapm.inkacourier_app.vistas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.dapm.inkacourier_app.R

/** Vista para dibujar la firma del receptor (RF-08). */
class FirmaView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val trazo = Path()
    private val pincel = Paint().apply {
        color = ContextCompat.getColor(context, R.color.firma_trazo)
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    var tieneTrazos = false
        private set

    /** Se invoca cuando la firma pasa de vacía a dibujada o al revés. */
    var alCambiarTrazos: ((Boolean) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(trazo, pincel)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                trazo.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                trazo.lineTo(event.x, event.y)
                if (!tieneTrazos) {
                    tieneTrazos = true
                    alCambiarTrazos?.invoke(true)
                }
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun borrar() {
        trazo.reset()
        tieneTrazos = false
        alCambiarTrazos?.invoke(false)
        invalidate()
    }

    /** Imagen de la firma sobre fondo blanco para asociarla al borrador. */
    fun obtenerFirma(): Bitmap {
        val imagen = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(imagen)
        lienzo.drawColor(Color.WHITE)
        lienzo.drawPath(trazo, pincel)
        return imagen
    }
}

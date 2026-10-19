package com.dapm.inkacourier_app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Recorridos de NAV-01 sobre la interfaz real (HU-01 y retornos de registro). */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @Before
    fun iniciar() {
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun hu01_seleccionarParadaMuestraDomicilioContactoVentanaYReferencias() {
        onView(withText("María Rojas")).perform(click())
        onView(withText("Detalle de parada")).check(matches(isDisplayed()))
        onView(withText("Av. Los Olivos 245")).check(matches(isDisplayed()))
        onView(withText("900 000 245")).check(matches(isDisplayed()))
        onView(withText("09:00 – 12:00")).check(matches(isDisplayed()))
        onView(withText("Portón verde. Interior 2, timbre del lado derecho. Fachada de dos pisos."))
            .check(matches(isDisplayed()))
        pressBack()
        onView(withText("Mis paradas")).check(matches(isDisplayed()))
    }

    @Test
    fun hu01_paradaContraentregaMuestraImporteYMedios() {
        onView(withText("Diego Torres")).perform(click())
        onView(withText("S/ 128.50")).check(matches(isDisplayed()))
        onView(withText("Efectivo · Yape · Plin")).check(matches(isDisplayed()))
    }

    @Test
    fun hu01_paradaEntregadaNoPermiteRegistrarDeNuevo() {
        onView(withText("Ana Mendoza")).perform(click())
        onView(withId(R.id.btnRegistrarRecepcion)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.btnRegistrarVisitaFallida)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun registroIncompletoNoPermiteGuardarYCancelarDescartaElBorrador() {
        onView(withText("María Rojas")).perform(click())
        onView(withId(R.id.btnRegistrarRecepcion)).perform(click())
        onView(withId(R.id.btnGuardarRecepcion)).check(matches(not(isEnabled())))
        onView(withId(R.id.edtNombreReceptor)).perform(typeText("Borrador"), closeSoftKeyboard())

        onView(withId(R.id.btnCancelar)).perform(click())
        onView(withText("Detalle de parada")).check(matches(isDisplayed()))

        onView(withId(R.id.btnRegistrarRecepcion)).perform(click())
        onView(withId(R.id.edtNombreReceptor)).check(matches(withText("")))
    }

    @Test
    fun visitaFallidaSinCausaNoPermiteRegistrar() {
        onView(withText("María Rojas")).perform(click())
        onView(withId(R.id.btnRegistrarVisitaFallida)).perform(click())
        onView(withText("Visita fallida")).check(matches(isDisplayed()))
        onView(withId(R.id.btnRegistrar)).check(matches(not(isEnabled())))
        pressBack()
        onView(withText("Detalle de parada")).check(matches(isDisplayed()))
    }
}

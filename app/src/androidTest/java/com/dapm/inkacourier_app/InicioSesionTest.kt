package com.dapm.inkacourier_app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dapm.inkacourier_app.vistas.ParadasFragment
import androidx.test.platform.app.InstrumentationRegistry
import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Inicio de sesión sobre la interfaz real: pantalla inicial, errores en el formulario y acceso. */
@RunWith(AndroidJUnit4::class)
class InicioSesionTest {

    private lateinit var escenario: ActivityScenario<MainActivity>

    @Before
    fun iniciar() {
        escenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun ingresar(usuario: String, contrasena: String) {
        if (usuario.isNotEmpty()) onView(withId(R.id.usuario)).perform(typeText(usuario))
        if (contrasena.isNotEmpty()) onView(withId(R.id.contrasena)).perform(typeText(contrasena))
        onView(withId(R.id.contrasena)).perform(closeSoftKeyboard())
        onView(withId(R.id.btnIniciarSesion)).perform(click())
    }

    @Test
    fun procesoNuevoMuestraElFormularioConContrasenaEnmascarada() {
        onView(withText("Acceso del repartidor")).check(matches(isDisplayed()))
        onView(withText("Mis paradas")).check(doesNotExist())
        onView(withId(R.id.contrasena))
            .check(matches(withInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)))
    }

    @Test
    fun ambosCamposVaciosIndicanCadaCampoObligatorio() {
        ingresar("", "")
        onView(withText("Completa los campos obligatorios")).check(matches(isDisplayed()))
        onView(withText("Ingresa tu usuario.")).check(matches(isDisplayed()))
        onView(withText("Ingresa tu contraseña.")).check(matches(isDisplayed()))
        onView(withText("Mis paradas")).check(doesNotExist())
    }

    @Test
    fun usuarioVacioSoloIndicaElUsuario() {
        ingresar("", "ruta01")
        onView(withText("Ingresa tu usuario.")).check(matches(isDisplayed()))
        onView(withText("Ingresa tu contraseña.")).check(doesNotExist())
        onView(withText("Mis paradas")).check(doesNotExist())
    }

    @Test
    fun contrasenaVaciaSoloIndicaLaContrasena() {
        ingresar("lucho", "")
        onView(withText("Ingresa tu contraseña.")).check(matches(isDisplayed()))
        onView(withText("Ingresa tu usuario.")).check(doesNotExist())
        onView(withText("Mis paradas")).check(doesNotExist())
    }

    @Test
    fun credencialesCruzadasMuestranErrorYPermanecenEnElFormulario() {
        ingresar("lucho", "ruta02")
        onView(withText("Usuario o contraseña incorrectos")).check(matches(isDisplayed()))
        onView(withText("Acceso del repartidor")).check(matches(isDisplayed()))
        onView(withText("Mis paradas")).check(doesNotExist())
    }

    @Test
    fun luchoVeSoloSusParadasYAtrasNoVuelveAlFormulario() {
        ingresar("lucho", "ruta01")
        onView(withText("Mis paradas")).check(matches(isDisplayed()))
        onView(withText("María Rojas")).check(matches(isDisplayed()))
        onView(withText("Elena Castro")).check(doesNotExist())

        // El formulario no está en la pila: Atrás desde Paradas sale de la app
        // (Android 12+ la envía al fondo en lugar de destruirla).
        escenario.onActivity { actividad ->
            assertEquals(0, actividad.supportFragmentManager.backStackEntryCount)
            assertTrue(actividad.supportFragmentManager.findFragmentById(R.id.contenedor) is ParadasFragment)
        }
        pressBackUnconditionally()
        val limite = System.currentTimeMillis() + 5_000
        while (escenario.state == Lifecycle.State.RESUMED || escenario.state == Lifecycle.State.STARTED) {
            if (System.currentTimeMillis() > limite) break
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        assertTrue("La app deja el primer plano", escenario.state.isAtLeast(Lifecycle.State.STARTED).not())
    }

    @Test
    fun rosaVeSoloSusParadas() {
        ingresar("rosa", "ruta02")
        onView(withText("Elena Castro")).check(matches(isDisplayed()))
        onView(withText("Hugo Ramírez")).check(matches(isDisplayed()))
        onView(withText("María Rojas")).check(doesNotExist())
        onView(withText("2 asignaciones")).check(matches(isDisplayed()))
    }

    @Test
    fun recrearLaActivityConservaLaSesionYLaPantalla() {
        ingresar("rosa", "ruta02")
        onView(withText("Elena Castro")).perform(click())
        onView(withText("Jr. Huáscar 512")).check(matches(isDisplayed()))

        escenario.recreate()
        onView(withText("Jr. Huáscar 512")).check(matches(isDisplayed()))
        onView(withText("Acceso del repartidor")).check(doesNotExist())
    }
}

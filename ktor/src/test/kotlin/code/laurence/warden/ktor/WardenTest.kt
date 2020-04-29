package code.laurence.warden.ktor

import assertk.assertThat
import assertk.assertions.isEqualTo
import code.laurence.warden.ktor.Warden.Feature.NOT_ENFORCED_MESSAGE
import codes.laurence.warden.Access
import codes.laurence.warden.AccessRequest
import codes.laurence.warden.AccessResponse
import codes.laurence.warden.enforce.NotAuthorizedException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

fun Application.testableAppDependencies() {

    val enforcementPointKtor = EnforcementPointKtor(listOf(
        mockk {
            every { checkAuthorized(AccessRequest(subject = mapOf("access" to "granted"))) } returns AccessResponse(
                Access.Granted,
                AccessRequest(subject = mapOf("returned" to "from policy"))
            )
            every { checkAuthorized(AccessRequest(subject = mapOf("access" to "denied"))) } returns AccessResponse(
                Access.Denied(mapOf("message" to "Auth Denied")),
                AccessRequest(subject = mapOf("returned" to "from policy"))
            )
        }
    )
    )
    install(Warden) {
        routePriorityStack = listOf(
            WardenRoute("/authorizationNotEnforced/IgnoredInConfig", WardenRouteBehaviour.ENFORCE)
        )
    }
    install(StatusPages) {
        exception<NotAuthorizedException> { cause ->
            call.respondText(
                cause.deniedProperties.getOrDefault("message", "No Denied message") as String,
                status = HttpStatusCode.Unauthorized
            )
        }
    }

    routing {
        route("/authorizationNotEnforced") {
            get("") {
                wardenCall {
                    call.respondText("You should not be able to get me. Not Enforced")
                }
            }
            get("/IgnoredInline") {
                wardenIgnore()
                call.respondText("You should see me, enforcement ignored")
            }
            get("/IgnoredInConfig") {
                wardenIgnore()
                call.respondText("You should see me, enforcement ignored")
            }
        }
        route("/authorizationEnforced") {
            get("/Granted") {
                wardenCall {
                    enforcementPointKtor.enforceAuthorization(AccessRequest(subject = mapOf("access" to "granted")))
                    call.respondText("You should see me")
                }
            }
            get("/Denied") {
                wardenCall {
                    enforcementPointKtor.enforceAuthorization(AccessRequest(subject = mapOf("access" to "denied")))
                    call.respondText("You should not be able to get me")
                }
            }
            get("/wardenCallNotCalled") {
                try {
                    enforcementPointKtor.enforceAuthorization(AccessRequest(subject = mapOf("access" to "granted")))
                    call.respondText("You should not see me due to an error")
                } catch (e: KtorEnforcementPointException) {
                    wardenIgnore()
                    call.respondText("Got a KtorEnforcementError")
                }
            }
        }
    }
}

class WardenTest {

    @Test
    fun `get - enforcement point not called - 401`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationNotEnforced")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertEquals(NOT_ENFORCED_MESSAGE, response.content)
            }
        }
    }

    @Test
    fun `get - enforcement point not called - ignored inline`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationNotEnforced/IgnoredInline")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("You should see me, enforcement ignored", response.content)
            }
            // Check that ignoring once does not effect other calls
            with(handleRequest(HttpMethod.Get, "/authorizationNotEnforced")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `get - enforcement point not called - ignored in config`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationNotEnforced/IgnoredInConfig")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("You should see me, enforcement ignored", response.content)
            }
            // Check that ignoring once does not effect other calls
            with(handleRequest(HttpMethod.Get, "/authorizationNotEnforced")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `get - enforcement point called - granted`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationEnforced/Granted")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("You should see me", response.content)
            }
        }
    }

    @Test
    fun `get - enforcement point called - denied`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationEnforced/Denied")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertEquals("Auth Denied", response.content)
            }
        }
    }

    @Test
    fun `get - enforcement point called - wardenCall not called`() {
        withTestApplication({ testableAppDependencies() }) {
            with(handleRequest(HttpMethod.Get, "/authorizationEnforced/wardenCallNotCalled")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Got a KtorEnforcementError", response.content)
            }
        }
    }

    @Test
    fun `routeStack - route not in stack - enforced`() {
        val stack: List<WardenRoute> = listOf()
        val actual = evaluateRoute(stack, "/something", HttpMethod.DefaultMethods.random())
        assertThat(actual).isEqualTo(WardenRouteBehaviour.ENFORCE)
    }

    @Test
    fun `routeStack - route matches - method does not`() {
        val stack: List<WardenRoute> = listOf(
            WardenRoute("/something", WardenRouteBehaviour.IGNORE, methods = setOf(HttpMethod.Get))
        )
        val actual = evaluateRoute(stack, "/something", HttpMethod.Put)
        assertThat(actual).isEqualTo(WardenRouteBehaviour.ENFORCE)
    }

    @Test
    fun `routeStack - route matches - method matches`() {
        val stack: List<WardenRoute> = listOf(
            WardenRoute("/something", WardenRouteBehaviour.IGNORE, methods = setOf(HttpMethod.Get))
        )
        val actual = evaluateRoute(stack, "/something", HttpMethod.Get)
        assertThat(actual).isEqualTo(WardenRouteBehaviour.IGNORE)
    }

    @Test
    fun `routeStack - route regex prefix matches - method matches`() {
        val stack: List<WardenRoute> = listOf(
            WardenRoute("/some.*", WardenRouteBehaviour.IGNORE, methods = setOf(HttpMethod.Get))
        )
        val actual = evaluateRoute(stack, "/something", HttpMethod.Get)
        assertThat(actual).isEqualTo(WardenRouteBehaviour.IGNORE)
    }

    @Test
    fun `routeStack - route non regex prefix does not match - method matches`() {
        val stack: List<WardenRoute> = listOf(
            WardenRoute("/some", WardenRouteBehaviour.IGNORE, methods = setOf(HttpMethod.Get))
        )
        val actual = evaluateRoute(stack, "/something", HttpMethod.Get)
        assertThat(actual).isEqualTo(WardenRouteBehaviour.ENFORCE)
    }

    @Test
    fun `routeStack - higher priority overrides lower match`() {
        val stack: List<WardenRoute> = listOf(
            WardenRoute("/some/thing", WardenRouteBehaviour.ENFORCE, methods = setOf(HttpMethod.Get)),
            WardenRoute("/some", WardenRouteBehaviour.IGNORE, methods = setOf(HttpMethod.Get))
        )
        val actual = evaluateRoute(stack, "/some/thing", HttpMethod.Get)
        assertThat(actual).isEqualTo(WardenRouteBehaviour.ENFORCE)
    }


}
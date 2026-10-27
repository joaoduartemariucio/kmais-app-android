package br.com.joaoduartemariucio.kmais_app_android.viewmodels

import br.com.joaoduartemariucio.kmais_app_android.calculations.assertDecimal
import br.com.joaoduartemariucio.kmais_app_android.calculations.day
import br.com.joaoduartemariucio.kmais_app_android.calculations.decimal
import br.com.joaoduartemariucio.kmais_app_android.calculations.full
import br.com.joaoduartemariucio.kmais_app_android.calculations.partial
import br.com.joaoduartemariucio.kmais_app_android.calculations.rule
import br.com.joaoduartemariucio.kmais_app_android.calculations.sample
import br.com.joaoduartemariucio.kmais_app_android.calculations.service
import br.com.joaoduartemariucio.kmais_app_android.calculations.testVehicleId
import br.com.joaoduartemariucio.kmais_app_android.calculations.utc
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import br.com.joaoduartemariucio.kmais_app_android.fakes.TestLocalizer
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.blocksSaving
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.message
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationIssueTests {
    private val readings = listOf(sample(0, 1_000.0), sample(20, 2_000.0))

    @Test
    fun `a missing odometer is required`() {
        assertEquals(listOf(ValidationIssue.OdometerRequired), ValidationIssue.odometer(null, day(10), readings))
    }

    @Test
    fun `an odometer between its neighbors has no issue`() {
        assertTrue(ValidationIssue.odometer(1_500.0, day(10), readings).isEmpty())
    }

    @Test
    fun `an odometer not above the previous reading warns`() {
        val issues = ValidationIssue.odometer(1_000.0, day(10), readings)

        assertEquals(listOf(ValidationIssue.OdometerNotAfterPrevious(1_000.0)), issues)
        assertFalse(issues.blocksSaving)
    }

    @Test
    fun `an odometer not below the next reading warns`() {
        val issues = ValidationIssue.odometer(2_000.0, day(10), readings)

        assertEquals(listOf(ValidationIssue.OdometerNotBeforeNext(2_000.0)), issues)
        assertFalse(issues.blocksSaving)
    }

    @Test
    fun `only the odometer order is a warning - everything else blocks saving`() {
        val warnings = listOf(ValidationIssue.OdometerNotAfterPrevious(1.0), ValidationIssue.OdometerNotBeforeNext(1.0))
        val errors =
            listOf(
                ValidationIssue.NameRequired,
                ValidationIssue.OdometerRequired,
                ValidationIssue.LitersRequired,
                ValidationIssue.LitersNotPositive,
                ValidationIssue.PriceRequired,
                ValidationIssue.PriceNotPositive,
                ValidationIssue.CostNegative,
                ValidationIssue.RuleWithoutInterval,
            )

        assertFalse(warnings.any { it.isBlocking })
        assertTrue(errors.all { it.isBlocking })
        assertTrue((warnings + ValidationIssue.NameRequired).blocksSaving)
    }

    @Test
    fun `every issue has its own message`() {
        val issues =
            listOf(
                ValidationIssue.NameRequired,
                ValidationIssue.OdometerRequired,
                ValidationIssue.OdometerNotAfterPrevious(1_000.0),
                ValidationIssue.OdometerNotBeforeNext(2_000.0),
                ValidationIssue.LitersRequired,
                ValidationIssue.LitersNotPositive,
                ValidationIssue.PriceRequired,
                ValidationIssue.PriceNotPositive,
                ValidationIssue.CostNegative,
                ValidationIssue.RuleWithoutInterval,
            )

        assertEquals(issues.size, issues.map { it.message(TestLocalizer()) }.toSet().size)
    }

    @Test
    fun `repository errors read as user-facing messages`() {
        val localizer = TestLocalizer()

        assertEquals("This vehicle no longer exists.", RepositoryError.VehicleNotFound(UUID.randomUUID()).message(localizer))
        assertEquals("This record no longer exists.", RepositoryError.EntryNotFound(UUID.randomUUID()).message(localizer))
        assertEquals("Could not save your changes.", RepositoryError.PersistenceFailed(IllegalStateException()).message(localizer))
        assertEquals("boom", IllegalStateException("boom").message(localizer))
        assertEquals("Could not save your changes.", IllegalStateException().message(localizer))
    }
}

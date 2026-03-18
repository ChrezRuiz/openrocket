package info.openrocket.core.motorsweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.AxialStage;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;

import org.junit.jupiter.api.Test;

public class MotorSweepBallastTest {

	private static ThrustCurveMotor buildMotor(String designation) {
		return new ThrustCurveMotor.Builder()
				.setManufacturer(Manufacturer.getManufacturer("TestMfg"))
				.setDesignation(designation)
				.setDescription("Test motor")
				.setMotorType(Motor.Type.SINGLE)
				.setStandardDelays(new double[] { 5 })
				.setDiameter(0.024)
				.setLength(0.07)
				.setTimePoints(new double[] { 0, 1, 2 })
				.setThrustPoints(new double[] { 0, 10, 0 })
				.setCGPoints(new CoordinateIF[] { Coordinate.NUL, Coordinate.NUL, Coordinate.NUL })
				.setDigest("digest-" + designation)
				.build();
	}

	private static Rocket buildSimpleRocket() {
		Rocket rocket = new Rocket();
		AxialStage stage = new AxialStage();
		stage.setName("Stage");
		rocket.addChild(stage);

		BodyTube body = new BodyTube(0.50, 0.024, 0.001);
		body.setName("Body Tube");
		body.setMotorMount(true);
		stage.addChild(body);

		return rocket;
	}

	// --- Analytical position calculation tests ---

	@Test
	public void testComputeBallastPositionKnownInputs() {
		// CG at 0.3m, total mass 1.0 kg, CP at 0.15m, refDiam 0.048m
		// Target caliber 1.0: targetCG = 0.15 - 1.0 * 0.048 = 0.102
		// ballastMass 0.5 kg
		// x_b = ((1.0 + 0.5) * 0.102 - 1.0 * 0.3) / 0.5
		// x_b = (0.153 - 0.3) / 0.5 = -0.294
		double pos = MotorSweepRunner.computeBallastPosition(
				0.3, 1.0, 0.15, 0.048, 0.5, 1.0);

		double expected = ((1.5 * 0.102) - (1.0 * 0.3)) / 0.5;
		assertEquals(expected, pos, 0.0001);
	}

	@Test
	public void testComputeBallastPositionZeroMassReturnsNaN() {
		double pos = MotorSweepRunner.computeBallastPosition(
				0.3, 1.0, 0.15, 0.048, 0.0, 1.0);
		assertTrue(Double.isNaN(pos));
	}

	@Test
	public void testComputeBallastPositionProducesTargetCaliber() {
		double cgX = 0.25;
		double totalMass = 0.8;
		double cpX = 0.15;
		double refDiam = 0.048;
		double ballastMass = 0.3;
		double targetCaliber = 1.5;

		double pos = MotorSweepRunner.computeBallastPosition(
				cgX, totalMass, cpX, refDiam, ballastMass, targetCaliber);

		// Verify: new CG should give target caliber
		double newCgX = (totalMass * cgX + ballastMass * pos) / (totalMass + ballastMass);
		double achievedCaliber = (cpX - newCgX) / refDiam;
		assertEquals(targetCaliber, achievedCaliber, 0.0001);
	}

	// --- Body bounds tests ---

	@Test
	public void testFindBodyBounds() {
		Rocket rocket = buildSimpleRocket();
		double[] bounds = MotorSweepRunner.findBodyBounds(rocket);

		assertNotNull(bounds);
		assertEquals(2, bounds.length);
		// Body tube starts at some position and is 0.5m long
		assertTrue(bounds[1] - bounds[0] >= 0.5);
	}

	@Test
	public void testFindBodyBoundsNoBodyTube() {
		Rocket rocket = new Rocket();
		AxialStage stage = new AxialStage();
		rocket.addChild(stage);

		double[] bounds = MotorSweepRunner.findBodyBounds(rocket);
		assertNull(bounds);
	}

	@Test
	public void testFindBodyTubeAt() {
		Rocket rocket = buildSimpleRocket();
		double[] bounds = MotorSweepRunner.findBodyBounds(rocket);

		// Position within tube should return it
		double midPoint = (bounds[0] + bounds[1]) / 2.0;
		BodyTube tube = MotorSweepRunner.findBodyTubeAt(rocket, midPoint);
		assertNotNull(tube);

		// Position outside should return null
		BodyTube outside = MotorSweepRunner.findBodyTubeAt(rocket, bounds[1] + 1.0);
		assertNull(outside);
	}

	// --- Infeasible result tests ---

	@Test
	public void testInfeasibleResultFields() {
		ThrustCurveMotor motor = buildMotor("A8");
		MotorSweepResult result = MotorSweepResult.infeasible(motor, "Exceeds max GLOM");

		assertTrue(result.isInfeasible());
		assertFalse(result.isSuccess());
		assertEquals("Exceeds max GLOM", result.getInfeasibleReason());
		assertTrue(Double.isNaN(result.getApogee()));
		assertTrue(Double.isNaN(result.getLaunchMass()));
	}

	@Test
	public void testSuccessWithBallastFields() {
		ThrustCurveMotor motor = buildMotor("A8");
		MotorSweepResult result = MotorSweepResult.success(motor, 300, 20, 15, 5.0,
				1.0, 50, 10, 0.2, 0.15, 1.5);

		assertTrue(result.isSuccess());
		assertFalse(result.isInfeasible());
		assertEquals(0.2, result.getBallastMass(), 0.001);
		assertEquals(0.15, result.getBallastPosition(), 0.001);
		assertEquals(1.5, result.getStabilityCaliber(), 0.001);
	}

	@Test
	public void testSuccessWithoutBallastDefaults() {
		ThrustCurveMotor motor = buildMotor("A8");
		MotorSweepResult result = MotorSweepResult.success(motor, 300, 20, 15, 5.0,
				1.0, 50, 10);

		assertEquals(0.0, result.getBallastMass(), 0.001);
		assertTrue(Double.isNaN(result.getBallastPosition()));
		assertTrue(Double.isNaN(result.getStabilityCaliber()));
		assertFalse(result.isInfeasible());
	}

	// --- Position at boundary edge cases ---

	@Test
	public void testPositionAtExactBoundaryIsValid() {
		// If the computed position equals the body tube boundary exactly,
		// it should be accepted (not infeasible)
		Rocket rocket = buildSimpleRocket();
		double[] bounds = MotorSweepRunner.findBodyBounds(rocket);

		// Position at fore boundary
		BodyTube atFore = MotorSweepRunner.findBodyTubeAt(rocket, bounds[0]);
		assertNotNull(atFore);

		// Position at aft boundary
		BodyTube atAft = MotorSweepRunner.findBodyTubeAt(rocket, bounds[1]);
		assertNotNull(atAft);
	}
}

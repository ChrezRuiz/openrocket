package info.openrocket.core.motorsweep;

import java.util.List;

import java.util.Iterator;

import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.rocketcomponent.AxialStage;
import info.openrocket.core.rocketcomponent.BodyTube;
import info.openrocket.core.rocketcomponent.FlightConfigurationId;
import info.openrocket.core.rocketcomponent.MassComponent;
import info.openrocket.core.rocketcomponent.MotorMount;
import info.openrocket.core.rocketcomponent.Rocket;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.rocketcomponent.position.AxialMethod;
import info.openrocket.core.util.BaseTestCase;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MotorSweepApplicatorTest extends BaseTestCase {

	private static ThrustCurveMotor buildMotor(String designation, double[] delays) {
		return new ThrustCurveMotor.Builder()
				.setManufacturer(Manufacturer.getManufacturer("TestMfg"))
				.setDesignation(designation)
				.setDescription("Test motor")
				.setMotorType(Motor.Type.SINGLE)
				.setStandardDelays(delays)
				.setDiameter(0.024)
				.setLength(0.07)
				.setTimePoints(new double[] { 0, 1, 2 })
				.setThrustPoints(new double[] { 0, 10, 0 })
				.setCGPoints(new CoordinateIF[] { Coordinate.NUL, Coordinate.NUL, Coordinate.NUL })
				.setDigest("digest-" + designation)
				.build();
	}

	private static Rocket buildRocketWithMount() {
		Rocket rocket = new Rocket();
		AxialStage stage = new AxialStage();
		stage.setName("Stage");
		rocket.addChild(stage);

		BodyTube body = new BodyTube(0.20, 0.012, 0.0003);
		body.setName("Body Tube");
		body.setMotorMount(true);
		stage.addChild(body);

		return rocket;
	}

	private static Rocket buildRocketWithoutMount() {
		Rocket rocket = new Rocket();
		AxialStage stage = new AxialStage();
		stage.setName("Stage");
		rocket.addChild(stage);

		BodyTube body = new BodyTube(0.20, 0.012, 0.0003);
		body.setName("Body Tube");
		body.setMotorMount(false);
		stage.addChild(body);

		return rocket;
	}

	@Test
	public void testFindAllMotorMounts() {
		Rocket rocket = buildRocketWithMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		assertEquals(1, mounts.size());
	}

	@Test
	public void testFindAllMotorMountsNone() {
		Rocket rocket = buildRocketWithoutMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		assertTrue(mounts.isEmpty());
	}

	@Test
	public void testGetDefaultEjectionDelay() {
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5, 7, 10 });
		assertEquals(5.0, MotorSweepApplicator.getDefaultEjectionDelay(motor), 0.001);
	}

	@Test
	public void testGetDefaultEjectionDelayEmpty() {
		ThrustCurveMotor motor = buildMotor("A8", new double[] {});
		assertEquals(0.0, MotorSweepApplicator.getDefaultEjectionDelay(motor), 0.001);
	}

	@Test
	public void testApplyToNewConfiguration() {
		Rocket rocket = buildRocketWithMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		MotorMount mount = mounts.get(0);
		ThrustCurveMotor motor = buildMotor("B6", new double[] { 5 });

		int configCountBefore = rocket.getFlightConfigurationCount();

		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyToNewConfiguration(rocket, mount, motor, 5.0);

		assertTrue(result.isSuccess());
		assertNotNull(result.getAppliedConfigId());
		assertEquals(configCountBefore + 1, rocket.getFlightConfigurationCount());

		// Verify motor is set on the mount for the new config
		FlightConfigurationId fcid = result.getAppliedConfigId();
		assertEquals(motor, mount.getMotorConfig(fcid).getMotor());
		assertEquals(5.0, mount.getMotorConfig(fcid).getEjectionDelay(), 0.001);
	}

	@Test
	public void testApplyToExistingConfiguration() {
		Rocket rocket = buildRocketWithMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		MotorMount mount = mounts.get(0);
		ThrustCurveMotor motor = buildMotor("C6", new double[] { 7 });

		// Create an existing configuration
		FlightConfigurationId existingFcid = new FlightConfigurationId();
		rocket.createFlightConfiguration(existingFcid);

		int configCountBefore = rocket.getFlightConfigurationCount();

		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyToExistingConfiguration(
						rocket, mount, motor, 7.0, existingFcid);

		assertTrue(result.isSuccess());
		assertEquals(existingFcid, result.getAppliedConfigId());
		// No new config should be created
		assertEquals(configCountBefore, rocket.getFlightConfigurationCount());

		// Verify motor is set
		assertEquals(motor, mount.getMotorConfig(existingFcid).getMotor());
		assertEquals(7.0, mount.getMotorConfig(existingFcid).getEjectionDelay(), 0.001);
	}

	@Test
	public void testApplyNullMotorReturnsError() {
		Rocket rocket = buildRocketWithMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		MotorMount mount = mounts.get(0);

		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyToNewConfiguration(rocket, mount, null, 0);

		assertFalse(result.isSuccess());
		assertNull(result.getAppliedConfigId());
	}

	@Test
	public void testApplyNullMountReturnsError() {
		Rocket rocket = buildRocketWithMount();
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5 });

		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyToNewConfiguration(rocket, null, motor, 5.0);

		assertFalse(result.isSuccess());
		assertNull(result.getAppliedConfigId());
	}

	@Test
	public void testApplyToNonexistentConfigReturnsError() {
		Rocket rocket = buildRocketWithMount();
		List<MotorMount> mounts = MotorSweepApplicator.findAllMotorMounts(rocket);
		MotorMount mount = mounts.get(0);
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5 });

		FlightConfigurationId fakeFcid = new FlightConfigurationId();

		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyToExistingConfiguration(
						rocket, mount, motor, 5.0, fakeFcid);

		assertFalse(result.isSuccess());
		assertNull(result.getAppliedConfigId());
	}

	// --- Ballast tests ---

	@Test
	public void testHasBallastDataTrue() {
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5 });
		MotorSweepResult result = MotorSweepResult.success(
				motor, 300, 10, 20, 5.0, 0.5, 100, 10,
				0.05, 0.10, 1.5);
		assertTrue(MotorSweepApplicator.hasBallastData(result));
	}

	@Test
	public void testHasBallastDataFalseZeroMass() {
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5 });
		MotorSweepResult result = MotorSweepResult.success(
				motor, 300, 10, 20, 5.0, 0.5, 100, 10);
		assertFalse(MotorSweepApplicator.hasBallastData(result));
	}

	@Test
	public void testHasBallastDataFalseNaNPosition() {
		ThrustCurveMotor motor = buildMotor("A8", new double[] { 5 });
		MotorSweepResult result = MotorSweepResult.success(
				motor, 300, 10, 20, 5.0, 0.5, 100, 10,
				0.05, Double.NaN, 1.5);
		assertFalse(MotorSweepApplicator.hasBallastData(result));
	}

	@Test
	public void testApplyBallast() {
		Rocket rocket = buildRocketWithMount();

		// Position 0.10 is midpoint of the 0.20 m body tube
		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyBallast(rocket, 0.05, 0.10, false);

		assertTrue(result.isSuccess());

		MassComponent found = MotorSweepApplicator.findExistingSweepBallast(rocket);
		assertNotNull(found);
		assertEquals("Sweep Ballast", found.getName());
		assertEquals(0.05, found.getComponentMass(), 0.001);
		assertEquals(AxialMethod.ABSOLUTE, found.getAxialMethod());
		assertEquals(0.10, found.getAxialOffset(), 0.001);
	}

	@Test
	public void testApplyBallastReplaceExisting() {
		Rocket rocket = buildRocketWithMount();

		// Add initial ballast
		MotorSweepApplicator.applyBallast(rocket, 0.03, 0.05, false);
		assertNotNull(MotorSweepApplicator.findExistingSweepBallast(rocket));

		// Replace with new ballast
		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyBallast(rocket, 0.07, 0.15, true);

		assertTrue(result.isSuccess());

		// Should only be one "Sweep Ballast"
		int count = countSweepBallasts(rocket);
		assertEquals(1, count);

		MassComponent found = MotorSweepApplicator.findExistingSweepBallast(rocket);
		assertEquals(0.07, found.getComponentMass(), 0.001);
		assertEquals(0.15, found.getAxialOffset(), 0.001);
	}

	@Test
	public void testApplyBallastAddSecond() {
		Rocket rocket = buildRocketWithMount();

		// Add initial ballast
		MotorSweepApplicator.applyBallast(rocket, 0.03, 0.05, false);

		// Add another without replacing
		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyBallast(rocket, 0.07, 0.15, false);

		assertTrue(result.isSuccess());

		// Should be two "Sweep Ballast" components
		int count = countSweepBallasts(rocket);
		assertEquals(2, count);
	}

	@Test
	public void testFindExistingSweepBallast() {
		Rocket rocket = buildRocketWithMount();
		assertNull(MotorSweepApplicator.findExistingSweepBallast(rocket));

		MotorSweepApplicator.applyBallast(rocket, 0.05, 0.10, false);
		assertNotNull(MotorSweepApplicator.findExistingSweepBallast(rocket));
	}

	@Test
	public void testApplyBallastNoBodyTube() {
		Rocket rocket = buildRocketWithMount();

		// Position 5.0 is well outside the 0.20 m body tube
		MotorSweepApplicator.ApplyResult result =
				MotorSweepApplicator.applyBallast(rocket, 0.05, 5.0, false);

		assertFalse(result.isSuccess());
	}

	private static int countSweepBallasts(Rocket rocket) {
		int count = 0;
		Iterator<RocketComponent> it = rocket.iterator(true);
		while (it.hasNext()) {
			RocketComponent c = it.next();
			if (c instanceof MassComponent
					&& "Sweep Ballast".equals(c.getName())) {
				count++;
			}
		}
		return count;
	}
}

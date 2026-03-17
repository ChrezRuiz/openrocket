package info.openrocket.core.motorsweep;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import info.openrocket.core.database.motor.ThrustCurveMotorSetDatabase;
import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MotorSweepFilterTest {

	private static ThrustCurveMotor buildMotor(String designation, String digest,
			Motor.Type type, double diameter, double length) {
		return new ThrustCurveMotor.Builder()
				.setManufacturer(Manufacturer.getManufacturer("TestMfg"))
				.setDesignation(designation)
				.setDescription("Test motor")
				.setMotorType(type)
				.setStandardDelays(new double[] { 5 })
				.setDiameter(diameter)
				.setLength(length)
				.setTimePoints(new double[] { 0, 1, 2 })
				.setThrustPoints(new double[] { 0, 10, 0 })
				.setCGPoints(new CoordinateIF[] { Coordinate.NUL, Coordinate.NUL, Coordinate.NUL })
				.setDigest(digest)
				.build();
	}

	@Test
	public void testFilterByDiameter() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		db.addMotor(buildMotor("A8", "d1", Motor.Type.SINGLE, 0.018, 0.07));
		db.addMotor(buildMotor("C6", "d2", Motor.Type.SINGLE, 0.024, 0.07));
		db.addMotor(buildMotor("G80", "d3", Motor.Type.SINGLE, 0.038, 0.07));

		MotorSweepFilter filter = new MotorSweepFilter(0.025, 1.0,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		assertEquals(2, result.size());
		assertTrue(result.stream().allMatch(m -> m.getDiameter() <= 0.025));
	}

	@Test
	public void testFilterByLength() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		db.addMotor(buildMotor("A8", "d1", Motor.Type.SINGLE, 0.018, 0.05));
		db.addMotor(buildMotor("B6", "d2", Motor.Type.SINGLE, 0.018, 0.09));
		db.addMotor(buildMotor("C6", "d3", Motor.Type.SINGLE, 0.018, 0.15));

		MotorSweepFilter filter = new MotorSweepFilter(1.0, 0.10,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		assertEquals(2, result.size());
		assertTrue(result.stream().allMatch(m -> m.getLength() <= 0.10));
	}

	@Test
	public void testFilterByType() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		db.addMotor(buildMotor("A8", "d1", Motor.Type.SINGLE, 0.018, 0.07));
		db.addMotor(buildMotor("D12", "d2", Motor.Type.RELOAD, 0.024, 0.07));
		db.addMotor(buildMotor("H128", "d3", Motor.Type.HYBRID, 0.038, 0.10));

		MotorSweepFilter filter = new MotorSweepFilter(1.0, 1.0,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		assertEquals(1, result.size());
		assertEquals("A8", result.get(0).getDesignation());
	}

	@Test
	public void testFilterCombined() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		// Passes all: SINGLE, 18mm, 5cm
		db.addMotor(buildMotor("A8", "d1", Motor.Type.SINGLE, 0.018, 0.05));
		// Fails type: RELOAD
		db.addMotor(buildMotor("B6", "d2", Motor.Type.RELOAD, 0.018, 0.05));
		// Fails diameter: 38mm
		db.addMotor(buildMotor("G80", "d3", Motor.Type.SINGLE, 0.038, 0.05));
		// Fails length: 20cm
		db.addMotor(buildMotor("C6", "d4", Motor.Type.SINGLE, 0.018, 0.20));

		MotorSweepFilter filter = new MotorSweepFilter(0.025, 0.10,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		assertEquals(1, result.size());
		assertEquals("A8", result.get(0).getDesignation());
	}

	@Test
	public void testFilterEmpty() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		db.addMotor(buildMotor("G80", "d1", Motor.Type.HYBRID, 0.038, 0.20));

		MotorSweepFilter filter = new MotorSweepFilter(0.025, 0.10,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testOneMotorPerSet() {
		ThrustCurveMotorSetDatabase db = new ThrustCurveMotorSetDatabase();
		// Two motors with same designation/manufacturer/diameter go into the same set
		db.addMotor(buildMotor("F12", "d1", Motor.Type.SINGLE, 0.024, 0.07));
		db.addMotor(buildMotor("F12", "d2", Motor.Type.SINGLE, 0.024, 0.07));

		// Verify they ended up in one set
		assertEquals(1, db.getMotorSets().size());
		assertEquals(2, db.getMotorSets().get(0).getMotors().size());

		MotorSweepFilter filter = new MotorSweepFilter(1.0, 1.0,
				EnumSet.of(Motor.Type.SINGLE));
		List<ThrustCurveMotor> result = filter.filter(db);

		// Filter should return only 1 motor (first from the set)
		assertEquals(1, result.size());
	}
}

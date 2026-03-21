package info.openrocket.core.motorsweep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import info.openrocket.core.motor.Manufacturer;
import info.openrocket.core.motor.Motor;
import info.openrocket.core.motor.ThrustCurveMotor;
import info.openrocket.core.simulation.FlightDataBranch;
import info.openrocket.core.simulation.FlightDataType;
import info.openrocket.core.util.Coordinate;
import info.openrocket.core.util.CoordinateIF;

public class MotorSweepResultFlightDataTest {

	private static ThrustCurveMotor buildMotor() {
		return new ThrustCurveMotor.Builder()
				.setManufacturer(Manufacturer.getManufacturer("TestMfg"))
				.setDesignation("T100")
				.setDescription("Test motor")
				.setMotorType(Motor.Type.SINGLE)
				.setStandardDelays(new double[] {5})
				.setDiameter(0.029)
				.setLength(0.1)
				.setTimePoints(new double[] {0, 1, 2})
				.setThrustPoints(new double[] {0, 100, 0})
				.setCGPoints(new CoordinateIF[] {
					Coordinate.NUL, Coordinate.NUL, Coordinate.NUL
				})
				.setDigest("test-digest-flight")
				.build();
	}

	private static FlightDataBranch buildBranch() {
		FlightDataBranch branch = new FlightDataBranch("test",
				FlightDataType.TYPE_TIME, FlightDataType.TYPE_ALTITUDE);
		branch.addPoint();
		branch.setValue(FlightDataType.TYPE_TIME, 0.0);
		branch.setValue(FlightDataType.TYPE_ALTITUDE, 0.0);
		branch.addPoint();
		branch.setValue(FlightDataType.TYPE_TIME, 5.0);
		branch.setValue(FlightDataType.TYPE_ALTITUDE, 300.0);
		branch.addPoint();
		branch.setValue(FlightDataType.TYPE_TIME, 10.0);
		branch.setValue(FlightDataType.TYPE_ALTITUDE, 0.0);
		branch.immute();
		return branch;
	}

	@Test
	public void testSuccessWithFlightData() {
		ThrustCurveMotor motor = buildMotor();
		FlightDataBranch branch = buildBranch();

		MotorSweepResult result = MotorSweepResult.success(motor, 300.0,
				50.0, 100.0, 5.0, 0.5, 150.0, 10.0,
				0.0, Double.NaN, Double.NaN, branch);

		assertTrue(result.hasFlightData());
		assertNotNull(result.getFlightDataBranch());
		assertEquals(branch, result.getFlightDataBranch());
	}

	@Test
	public void testSuccessWithoutFlightData() {
		ThrustCurveMotor motor = buildMotor();

		MotorSweepResult result = MotorSweepResult.success(motor, 300.0,
				50.0, 100.0, 5.0, 0.5, 150.0, 10.0);

		assertFalse(result.hasFlightData());
		assertNull(result.getFlightDataBranch());
	}

	@Test
	public void testErrorHasNoFlightData() {
		ThrustCurveMotor motor = buildMotor();

		MotorSweepResult result = MotorSweepResult.error(motor, "Sim failed");

		assertFalse(result.hasFlightData());
		assertNull(result.getFlightDataBranch());
	}

	@Test
	public void testInfeasibleHasNoFlightData() {
		ThrustCurveMotor motor = buildMotor();

		MotorSweepResult result = MotorSweepResult.infeasible(motor,
				"Ballast unrealizable");

		assertFalse(result.hasFlightData());
		assertNull(result.getFlightDataBranch());
	}

	@Test
	public void testFlightDataBranchContainsTimeSeries() {
		ThrustCurveMotor motor = buildMotor();
		FlightDataBranch branch = buildBranch();

		MotorSweepResult result = MotorSweepResult.success(motor, 300.0,
				50.0, 100.0, 5.0, 0.5, 150.0, 10.0,
				0.0, Double.NaN, Double.NaN, branch);

		FlightDataBranch retrieved = result.getFlightDataBranch();
		List<Double> times = retrieved.get(FlightDataType.TYPE_TIME);
		List<Double> altitudes = retrieved.get(FlightDataType.TYPE_ALTITUDE);

		assertEquals(3, times.size());
		assertEquals(0.0, times.get(0), 1e-9);
		assertEquals(5.0, times.get(1), 1e-9);
		assertEquals(10.0, times.get(2), 1e-9);

		assertEquals(3, altitudes.size());
		assertEquals(0.0, altitudes.get(0), 1e-9);
		assertEquals(300.0, altitudes.get(1), 1e-9);
		assertEquals(0.0, altitudes.get(2), 1e-9);
	}
}

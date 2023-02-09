package raidzero.robot.submodules;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Timer;

import com.ctre.phoenix.sensors.WPI_Pigeon2;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.PathPlannerTrajectory.PathPlannerState;

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;

import raidzero.robot.Constants;
import raidzero.robot.Constants.SwerveConstants;
import raidzero.robot.Constants.DriveConstants;
import raidzero.robot.dashboard.Tab;

public class Swerve extends Submodule {

    private class WPI_Pigeon2_Helper extends WPI_Pigeon2 {
        public WPI_Pigeon2_Helper(int deviceNumber, String canbus) {
            super(deviceNumber, canbus);
        }

        public double getAngle() {
            return -super.getAngle();
        }
    }

    private enum ControlState {
        OPEN_LOOP, PATHING, AUTO_AIM
    };

    public enum AutoAimLocation {
        BLL, BLM, BLR, BML, BMM, BMR, BRL, BRM, BRR,
        RLL, RLM, RLR, RML, RMM, RMR, RRL, RRM, RRR,
        B_LOAD,
        R_LOAD
    };

    private static Swerve instance = null;

    public static Swerve getInstance() {
        if (instance == null) {
            instance = new Swerve();
        }
        return instance;
    }

    private Swerve() {
    }

    private SwerveModule topRightModule = new SwerveModule();
    private SwerveModule topLeftModule = new SwerveModule();
    private SwerveModule bottomLeftModule = new SwerveModule();
    private SwerveModule bottomRightModule = new SwerveModule();

    private WPI_Pigeon2_Helper pigeon;

    private SwerveDrivePoseEstimator odometry;
    private Pose2d currentPose;
    private Pose2d prevPose;
    private Field2d fieldPose = new Field2d();

    // private HolonomicDriveController pathController;
    private PathPlannerTrajectory currentTrajectory;
    // private Rotation2d targetAngle;
    private PIDController xController, yController, thetaController;
    private Timer timer = new Timer();

    private Pose2d desiredAutoAimPose;
    private PIDController autoAimXController, autoAimYController, autoAimThetaController;

    private ControlState controlState = ControlState.OPEN_LOOP;

    public void onStart(double timestamp) {
        controlState = ControlState.OPEN_LOOP;
    }

    public void onInit() {
        pigeon = new WPI_Pigeon2_Helper(0, Constants.CANBUS_STRING);
        Shuffleboard.getTab(Tab.MAIN).add("Pigey", pigeon).withSize(2, 2).withPosition(4, 4);

        topRightModule.onInit(
                SwerveConstants.TOP_RIGHT_THROTTLE_ID,
                SwerveConstants.TOP_RIGHT_ROTOR_ID,
                SwerveConstants.TOP_RIGHT_ROTOR_OFFSET,
                1);
        topLeftModule.onInit(
                SwerveConstants.TOP_LEFT_THROTTLE_ID,
                SwerveConstants.TOP_LEFT_ROTOR_ID,
                SwerveConstants.TOP_LEFT_ROTOR_OFFSET,
                2);
        bottomLeftModule.onInit(
                SwerveConstants.REAR_LEFT_THROTTLE_ID,
                SwerveConstants.REAR_LEFT_ROTOR_ID,
                SwerveConstants.REAR_LEFT_ROTOR_OFFSET,
                3);
        bottomRightModule.onInit(
                SwerveConstants.REAR_RIGHT_THROTTLE_ID,
                SwerveConstants.REAR_RIGHT_ROTOR_ID,
                SwerveConstants.REAR_RIGHT_ROTOR_OFFSET,
                4);

        odometry = new SwerveDrivePoseEstimator(
                SwerveConstants.KINEMATICS,
                Rotation2d.fromDegrees(pigeon.getAngle()),
                this.getModulePositions(),
                DriveConstants.STARTING_POSE,
                DriveConstants.STATE_STDEVS_MATRIX,
                DriveConstants.VISION_STDEVS_MATRIX);

        // pathController = new HolonomicDriveController(
        // new PIDController(SwerveConstants.XCONTROLLER_KP, 0, 0),
        // new PIDController(SwerveConstants.YCONTROLLER_KP, 0, 0),
        // new ProfiledPIDController(SwerveConstants.THETACONTROLLER_KP, 0, 0,
        // SwerveConstants.THETACONTROLLER_CONSTRAINTS
        // )
        // );
        xController = new PIDController(SwerveConstants.XCONTROLLER_KP, 0, 0);
        yController = new PIDController(SwerveConstants.YCONTROLLER_KP, 0, 0);
        thetaController = new PIDController(SwerveConstants.THETACONTROLLER_KP, 0, 0);

        autoAimXController = new PIDController(0, 0, 0);
        autoAimYController = new PIDController(0, 0, 0);
        autoAimThetaController = new PIDController(0, 0, 0);
        autoAimThetaController.enableContinuousInput(0, 360);
        autoAimXController.setTolerance(0);
        autoAimYController.setTolerance(0);
        autoAimThetaController.setTolerance(0);

        zero();
        prevPose = new Pose2d();
    }

    @Override
    public void update(double timestamp) {
        if (controlState == ControlState.PATHING) {
            updatePathing();
        } else if (controlState == ControlState.AUTO_AIM) {
            updateAutoAim();
        }
        topRightModule.update(timestamp);
        topLeftModule.update(timestamp);
        bottomLeftModule.update(timestamp);
        bottomRightModule.update(timestamp);

        prevPose = currentPose;
        currentPose = updateOdometry();
        fieldPose.setRobotPose(currentPose);

        // This needs to be moved somewhere else.....
        SmartDashboard.putData(fieldPose);

        // xPositionEntry.setDouble(currentPose.getX());
        // yPositionEntry.setDouble(currentPose.getY());
    }

    /**
     * Runs components in the submodule that have continuously changing inputs.
     */
    @Override
    public void run() {
        topRightModule.run();
        topLeftModule.run();
        bottomLeftModule.run();
        bottomRightModule.run();
    }

    @Override
    public void stop() {
        controlState = ControlState.OPEN_LOOP;
        topRightModule.stop();
        topLeftModule.stop();
        bottomLeftModule.stop();
        bottomRightModule.stop();
    }

    /**
     * Resets the sensor(s) to zero.
     */
    @Override
    public void zero() {
        zeroHeading();
        setPose(new Pose2d());
        topRightModule.zero();
        topLeftModule.zero();
        bottomLeftModule.zero();
        bottomRightModule.zero();
    }

    public SwerveModulePosition[] getModulePositions() {
        return new SwerveModulePosition[] {
                topLeftModule.getModulePosition(),
                topRightModule.getModulePosition(),
                bottomLeftModule.getModulePosition(),
                bottomRightModule.getModulePosition()
        };
    }

    /**
     * Zeroes the heading of the swerve.
     */
    public void zeroHeading() {
        pigeon.setYaw(0, Constants.TIMEOUT_MS);
    }

    public void setPose(Pose2d pose) {
        odometry.resetPosition(Rotation2d.fromDegrees(pigeon.getAngle()), getModulePositions(), pose);
    }

    public Pose2d getPose() {
        return odometry.getEstimatedPosition();
    }

    public SwerveDrivePoseEstimator getPoseEstimator() {
        return odometry;
    }

    public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds,
            Matrix<N3, N1> visionMeasurementStdDevs) {
        try {
            visionMeasurementStdDevs = new MatBuilder<N3, N1>(Nat.N3(), Nat.N1()).fill(0.2, 0.2, 0.1);
            odometry.addVisionMeasurement(visionRobotPoseMeters, timestampSeconds, visionMeasurementStdDevs);
        } catch (Exception e) {
            System.out.println("Cholesky decomposition failed, reverting...:");
            pigeon.setYaw(visionRobotPoseMeters.getRotation().getDegrees());
            setPose(visionRobotPoseMeters);
        }
    }

    public Pose2d getPrevPose() {
        return prevPose;
    }

    public SwerveDriveKinematics getKinematics() {
        return SwerveConstants.KINEMATICS;
    }

    private Pose2d updateOdometry() {
        try {
            return odometry.update(
                    Rotation2d.fromDegrees(pigeon.getAngle()),
                    getModulePositions());
        } catch (Exception e) {
            System.out.println(e);
            return odometry.getEstimatedPosition();
        }

    }

    public void drive(double xSpeed, double ySpeed, double angularSpeed, boolean fieldOriented) {
        boolean ignoreAngle = false;
        if (Math.abs(xSpeed) < 0.1 && Math.abs(ySpeed) < 0.1 && Math.abs(angularSpeed) < 0.1) {
            ignoreAngle = true;
        }
        var targetState = SwerveConstants.KINEMATICS.toSwerveModuleStates(
                fieldOriented
                        ? ChassisSpeeds.fromFieldRelativeSpeeds(
                                xSpeed, ySpeed, angularSpeed,
                                Rotation2d.fromDegrees(pigeon.getAngle()))
                        : new ChassisSpeeds(xSpeed, ySpeed, angularSpeed));
        SwerveDriveKinematics.desaturateWheelSpeeds(targetState, SwerveConstants.MAX_DRIVE_VEL_MPS);
        topLeftModule.setTargetState(targetState[0], ignoreAngle, true, true);
        topRightModule.setTargetState(targetState[1], ignoreAngle, true, true);
        bottomLeftModule.setTargetState(targetState[2], ignoreAngle, true, true);
        bottomRightModule.setTargetState(targetState[3], ignoreAngle, true, true);
    }

    public void followPath(PathPlannerTrajectory trajectory) {
        if (controlState == ControlState.PATHING) {
            return;
        }
        controlState = ControlState.PATHING;
        currentTrajectory = trajectory;

        timer.reset();
        timer.start();
    }

    // private void updatePathing() {
    // var state = currentTrajectory.sample(timer.get());
    // var chassisSpeed = pathController.calculate(currentPose, state, targetAngle);
    // var targetState =
    // SwerveConstants.KINEMATICS.toSwerveModuleStates(chassisSpeed);
    // topLeftModule.setTargetState(targetState[0], false, false);
    // topRightModule.setTargetState(targetState[1], false, false);
    // bottomLeftModule.setTargetState(targetState[2], false, false);
    // bottomRightModule.setTargetState(targetState[3], false, false);
    // }

    /**
     * A better updatePathing(), featuring:
     * - actually allows the robot to turn
     * - actually reads turn changes from the pathplanner trajectory
     * - fully feedback, no more weird feedforward stuff that doesnt actually work
     */
    private void updatePathing() {
        PathPlannerState state = (PathPlannerState) currentTrajectory.sample(timer.get());
        ChassisSpeeds desiredSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                xController.calculate(currentPose.getX(), state.poseMeters.getX()),
                yController.calculate(currentPose.getY(), state.poseMeters.getY()),
                thetaController.calculate(currentPose.getRotation().getRadians(), state.holonomicRotation.getRadians()),
                currentPose.getRotation());
        SwerveModuleState[] desiredState = SwerveConstants.KINEMATICS.toSwerveModuleStates(desiredSpeeds);
        topLeftModule.setTargetState(desiredState[0], false, true, false);
        topRightModule.setTargetState(desiredState[1], false, true, false);
        bottomLeftModule.setTargetState(desiredState[2], false, true, false);
        bottomRightModule.setTargetState(desiredState[3], false, true, false);
    }

    public boolean isFinishedPathing() {
        return timer.hasElapsed(currentTrajectory.getTotalTimeSeconds());
    }

    public void autoAim(AutoAimLocation location) {
        controlState = ControlState.AUTO_AIM;
        switch (location) {
            case BLL:
                break;
            case BLM:
                break;
            case BLR:
                break;
            case BML:
                break;
            case BMM:
                break;
            case BMR:
                break;
            case BRL:
                break;
            case BRM:
                break;
            case BRR:
                break;
            case B_LOAD:
                break;
            default:
                break;
        }
    }

    public void updateAutoAim() {
        ChassisSpeeds desiredSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                xController.calculate(getPose().getX(), desiredAutoAimPose.getX()),
                yController.calculate(getPose().getY(), desiredAutoAimPose.getY()),
                thetaController.calculate(getPose().getRotation().getDegrees(),
                        desiredAutoAimPose.getRotation().getDegrees()),
                getPose().getRotation());
        SwerveModuleState[] desiredState = SwerveConstants.KINEMATICS.toSwerveModuleStates(desiredSpeeds);
        topLeftModule.setTargetState(desiredState[0], false, true, false);
        topRightModule.setTargetState(desiredState[1], false, true, false);
        bottomLeftModule.setTargetState(desiredState[2], false, true, false);
        bottomRightModule.setTargetState(desiredState[3], false, true, false);
    }

    public void testModule(int quadrant, double motorOutput, double rotorOutput) {
        System.out.println("Testing Q" + quadrant + ": motor=" + motorOutput + " rotor=" + rotorOutput);
        if (quadrant == 1) {
            topRightModule.testMotorAndRotor(motorOutput, rotorOutput);
        } else if (quadrant == 2) {
            topLeftModule.testMotorAndRotor(motorOutput, rotorOutput);
        } else if (quadrant == 3) {
            bottomLeftModule.testMotorAndRotor(motorOutput, rotorOutput);
        } else {
            bottomRightModule.testMotorAndRotor(motorOutput, rotorOutput);
        }
    }

    public void setThrottleRampRate(double val) {
        topRightModule.setRotorRampRate(val);
        topLeftModule.setRotorRampRate(val);
        bottomRightModule.setRotorRampRate(val);
        bottomLeftModule.setRotorRampRate(val);
    }
}

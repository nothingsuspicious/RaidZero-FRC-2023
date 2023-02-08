package raidzero.robot;

import java.nio.file.Path;

import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.PneumaticsModuleType;

public class Constants {
    /**
     * Swerve Constants
     */
    public static final class SwerveConstants {
        public static final double kOpenLoopRampRate = 0.0;
        public static final double kClosedLoopRampRate = 0.0;

        /** Device IDs */
        public static final int TOP_LEFT_THROTTLE_ID = 0;
        public static final int TOP_RIGHT_THROTTLE_ID = 0;
        public static final int REAR_LEFT_THROTTLE_ID = 0;
        public static final int REAR_RIGHT_THROTTLE_ID = 0;

        public static final int TOP_LEFT_ROTOR_ID = 0;
        public static final int TOP_RIGHT_ROTOR_ID = 0;
        public static final int REAR_LEFT_ROTOR_ID = 0;
        public static final int REAR_RIGHT_ROTOR_ID = 0;

        public static final int TOP_LEFT_ROTOR_OFFSET = 0;
        public static final int TOP_RIGHT_ROTOR_OFFSET = 0;
        public static final int REAR_LEFT_ROTOR_OFFSET = 0;
        public static final int REAR_RIGHT_ROTOR_OFFSET = 0;

        public static final double THROTTLE_REDUCTION = (14.0 / 50.0) * (28.0 / 16.0) * (15.0 / 45.0);
        public static final double ROTOR_REDUCTION = (14.0 / 50.0) * (10.0 / 60.0);
        public static final double WHEEL_DIAMETER_METERS = 0.1016; 
        public static final double MAX_VEL_MPS = 4.959668;

        // 20.75 OR 22.75 inches
        public static final double TRACKWIDTH_METERS = Units.inchesToMeters(22.75); 
        public static final double WHEELBASE_METERS = Units.inchesToMeters(22.75);
        public static final double ROBOT_RADIUS_METERS = 0.40863; 

        public static final SwerveDriveKinematics KINEMATICS = new SwerveDriveKinematics(
            // Front left
            new Translation2d(TRACKWIDTH_METERS / 2.0, WHEELBASE_METERS / 2.0),
            // Front right
            new Translation2d(TRACKWIDTH_METERS / 2.0, -WHEELBASE_METERS / 2.0),
            // Back left
            new Translation2d(-TRACKWIDTH_METERS / 2.0, WHEELBASE_METERS / 2.0),
            // Back right
            new Translation2d(-TRACKWIDTH_METERS / 2.0, -WHEELBASE_METERS / 2.0)
        );

        /** 254 Pathing Constants (smooth): */
        public static final double MAX_DRIVE_VEL = MAX_VEL_MPS * 0.7;
        public static final double MAX_DRIVE_ACCEL = MAX_DRIVE_VEL;
        public static final double MAX_STEERING_VEL = Units.degreesToRadians(750);

        /** 254 Pathing Constants (fast): */
        // public static final double MAX_DRIVE_VEL = MAX_VEL_MPS;
        // public static final double MAX_DRIVE_ACCEL = MAX_DRIVE_VEL / 0.2;
        // public static final double MAX_STEERING_VEL = Units.degreesToRadians(1000);
        
        /** 254 Module Constants */
        public static final int ROTOR_POSITION_PID_SLOT = 0;
        public static final double ROTOR_KP = 0.75;
        public static final double ROTOR_KI = 0;
        public static final double ROTOR_KD = 15;

        public static final int THROTTLE_VELOCITY_PID_SLOT = 0;
        public static final double THROTTLE_KP = 0.1;
        public static final double THROTTLE_KI = 0.0;
        public static final double THROTTLE_KD = 0.01;
        public static final double THROTTLE_KF = 1023 / (MAX_VEL_MPS / (Math.PI * WHEEL_DIAMETER_METERS * THROTTLE_REDUCTION / 2048.0 * 10));

        // Using SDS 6.75 ratio
        public static final double THROTTLE_TICKS_TO_METERS = Math.PI * WHEEL_DIAMETER_METERS / (2048 * (1/THROTTLE_REDUCTION));
        public static final double CANCODER_TO_DEGREES = 360.0 / 4096.0;

        public static final boolean MOTOR_INVERSION = false;
        public static final boolean ROTOR_INVERSION = true;
        public static final boolean ROTOR_INVERT_SENSOR_PHASE = true;

        /** Current Limits */
        public static final SupplyCurrentLimitConfiguration ROTOR_CURRENT_LIMIT = 
            new SupplyCurrentLimitConfiguration(true, 25, 40, 0.1);
        public static final SupplyCurrentLimitConfiguration THROTTLE_CURRENT_LIMIT = 
            new SupplyCurrentLimitConfiguration(true, 35, 60, 0.1);
    }

    public static final class DriveConstants{
        public static final Rotation2d STARTING_ROTATION = new Rotation2d(0.0);
        public static final Pose2d STARTING_POSE = new Pose2d(0.5,3.0, STARTING_ROTATION);
        private static final double MAX_ACCEL_DISTANCE = 6.0*Math.pow(TIMEOUT_S,2);
        private static final double GYRO_ERROR_DEGREES_TIMEOUT = (0.4/SECONDS_IN_MINUTE)*TIMEOUT_S;
        public static final double CONFIDENCE_TO_ERROR = 1.0;
        public static final Matrix<N3,N1> STATE_STDEVS_MATRIX = new MatBuilder<N3,N1>(Nat.N3(),Nat.N1()).fill(
            MAX_ACCEL_DISTANCE,MAX_ACCEL_DISTANCE, MAX_ACCEL_DISTANCE/SwerveConstants.ROBOT_RADIUS_METERS);
        public static final Matrix<N1,N1> ANGLE_STDEVS_MATRIX = new MatBuilder<N1,N1>(Nat.N1(),Nat.N1()).fill(GYRO_ERROR_DEGREES_TIMEOUT);
        public static final Matrix<N3,N1> VISION_STDEVS_MATRIX = new MatBuilder<N3,N1>(Nat.N3(),Nat.N1()).fill(1.0,1.0,1.0);
    }

    public class PathingConstants {
        public static final int BASE_TRAJ_PERIOD_MS = 0;
        public static final int MIN_POINTS_IN_TALON = 10;
        public static final int TRANSMIT_PERIOD_MS = 20;
    }

    public static final class IntakeConstants {}

    

    public static final class VisionConstants{
        public static final String NAME = "SmartDashboard";
        public static final String APRILTAGFAMILY = "tag36h11";
        private static final String APRILTAGFILENAME = "AprilTagPoses.json";
        public static final Path APRILTAGPATH = Filesystem.getDeployDirectory().toPath().resolve(APRILTAGFILENAME);
        private static final double CAMERAXDISPLACEMENT = 0.0772;
        private static final double CAMERAYDISPLACEMENT = 0.3429;
        private static final double CAMERAZDISPLACEMENT = 0.56198;
        private static final Rotation2d[] CAMERAANGLES = {new Rotation2d(0),new Rotation2d(Math.PI)};
        
        // private static final double[][] CAMERALOCATIONS = {{1,2,3},{3,4,5}};
        private static final Pose2d[] CAMERALOCATIONS = {new Pose2d(CAMERAXDISPLACEMENT, -CAMERAYDISPLACEMENT, CAMERAANGLES[0]),
            new Pose2d(CAMERAXDISPLACEMENT, -CAMERAYDISPLACEMENT, CAMERAANGLES[1])};
    
        public static final double ANGLEHISTSECS = 2.0;
        public static final double DISTANCETOLERANCE = 3.0;
        //public static final Pose2d[] APRILTAG_POSE2DS = {new Pose2d(1, 1, new Rotation2d(.5))};
        // public final Pose2d[] APRILTAG_POSE2DS = JSONTools.GenerateAprilTagPoses(APRILTAGPATH);
        Path trajectoryFilePath = Filesystem.getDeployDirectory().toPath().resolve("paths/");
    }

    public static final class LimelightConstants {
        public static final String NAME = "limelight";

        public static final double MOUNTING_ANGLE = 0.0; // in degrees
        public static final double MOUNTING_HEIGHT = 0.0; // in meters
    }

    public static final class ArmConstants {
        public static final int LOWER_LEADER_ID = 0;
        public static final int LOWER_FOLLOWER_ID = 0;
        public static final int UPPER_LEADER_ID = 0;
        public static final int UPPER_FOLLOWER_ID = 0;

        public static final boolean LOWER_MOTOR_INVERSION = false;
        public static final boolean UPPER_MOTOR_INVERSION = false;

        public static final int LOWER_CURRENT_LIMIT = 35;
        public static final int UPPER_CURRENT_LIMIT = 35;

        public static final double LOWER_ZERO_OFFSET = 0.0;
        public static final double UPPER_ZERO_OFFSET = 0.0;

        public static final boolean LOWER_ENCODER_INVERSION = false;
        public static final boolean UPPER_ENCODER_INVERSION = false;

        public static final int LOWER_SMART_MOTION_SLOT = 0;
        public static final int UPPER_SMART_MOTION_SLOT = 0;

        public static final double LOWER_KF = 0.0;
        public static final double LOWER_KP = 0.0;
        public static final double LOWER_KI = 0.0;
        public static final double LOWER_KD = 0.0;
        public static final double LOWER_ARBITRARY_FF = 0.0;
        public static final double LOWER_MIN_VEL = 0.0;
        public static final double LOWER_MAX_VEL = 0.0;
        public static final double LOWER_MAX_ACCEL = 0.0;
        public static final double LOWER_MIN_ERROR = 0.0;
        public static final double UPPER_KF = 0.0;
        public static final double UPPER_KP = 0.0;
        public static final double UPPER_KI = 0.0;
        public static final double UPPER_KD = 0.0;
        public static final double UPPER_ARBITRARY_FF = 0.0;
        public static final double UPPER_MIN_VEL = 0.0;
        public static final double UPPER_MAX_VEL = 0.0;
        public static final double UPPER_MAX_ACCEL = 0.0;
        public static final double UPPER_MIN_ERROR = 0.0;

        public static final double PID_WRAPPING_MIN = 0.0;
        public static final double PID_WRAPPING_MAX = 360.0;
    }

    public static final double JOYSTICK_DEADBAND = 0.07;
    public static final int TIMEOUT_MS = 20;
    public static final double TIMEOUT_S = TIMEOUT_MS/1000.0f;
    public static final int SECONDS_IN_MINUTE = 60;
    public static final double SQRTTWO = Math.sqrt(2);
    public static final String CANBUS_STRING = "seCANdary";
    public static final PneumaticsModuleType PNEUMATICS_MODULE_TYPE = PneumaticsModuleType.REVPH;
    public static final double VOLTAGE_COMP = 12.0;
}

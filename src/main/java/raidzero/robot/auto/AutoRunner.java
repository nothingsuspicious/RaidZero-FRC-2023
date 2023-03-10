package raidzero.robot.auto;

import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;

import raidzero.robot.auto.sequences.*;
import raidzero.robot.dashboard.Tab;
import raidzero.robot.submodules.Swerve;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

/**
 * Class that manages autonomous sequences.
 */
public class AutoRunner {

    private SendableChooser<AutoSequence> chooser;
    private static final Swerve swerve = Swerve.getInstance();

    private AutoSequence selectedSequence;

    private AutoSequence[] availableSequences = {
            new TestSequence(),
            new EmptySequence(),
            new SafetySequence(),
            new SingleConeClimbSequence(),
            new SingleConeBalanceSequence(),
            new TwoConeClimbSequence(),
            new ConeCubeSequence()
    };

    public AutoRunner() {
        chooser = new SendableChooser<>();
        chooser.setDefaultOption("None", new EmptySequence());
        for (AutoSequence sequence : availableSequences) {
            chooser.addOption(sequence.getName(), sequence);
        }
        Shuffleboard.getTab(Tab.MAIN).add("Auton", chooser).withSize(2, 1).withPosition(2, 3);
        // SmartDashboard.putData("Auton Selection", chooser);
    }

    /**
     * Reads the selected autonomous sequence from the SendableChooser.
     */
    public void readSendableSequence() {
        selectSequence(chooser.getSelected());
    }

    /**
     * Selects an autonomous sequence to run.
     * 
     * @param name the exact name of the autonomous sequence to run
     */
    public void selectSequence(String name) {
        for (AutoSequence sequence : availableSequences) {
            if (sequence.getName().equalsIgnoreCase(name)) {
                selectedSequence = sequence;
                return;
            }
        }
    }

    /**
     * Selects an autonomous sequence to run.
     * 
     * @param sequence the autonomous sequence to run
     */
    public void selectSequence(AutoSequence sequence) {
        selectedSequence = sequence;
    }

    /**
     * Returns the currently selected autonomous sequence.
     * 
     * @return the selected sequence - null means no sequence.
     */
    public AutoSequence getSelectedSequence() {
        return selectedSequence;
    }

    /**
     * Starts the selected autonomous sequence.
     */
    public void start() {
        if (selectedSequence != null) {
            System.out.println(
                    "[Auto] Starting auto sequence '" + selectedSequence.getName() + "'...");
            selectedSequence.run();
        }
    }

    /**
     * Stops the selected autonomous sequence.
     */
    public void stop() {
        if (selectedSequence != null) {
            System.out.println(
                    "[Auto] Stopping auto sequence '" + selectedSequence.getName() + "'...");
            selectedSequence.stop();
            Alliance alliance = DriverStation.getAlliance();
        }
    }

    /**
     * Updates the selected autonomous sequence.
     * 
     * @param timestamp
     */
    public void onLoop(double timestamp) {
        if (selectedSequence != null) {
            selectedSequence.onLoop(timestamp);
        }
    }
}

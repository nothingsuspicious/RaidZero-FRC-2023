package raidzero.robot.auto.actions;

import edu.wpi.first.wpilibj.Timer;

import raidzero.robot.submodules.Intake;
import raidzero.robot.submodules.Swerve;

public class AutoBalanceAction implements Action {
    private static final Swerve mSwerve = Swerve.getInstance();
    private Timer timer = new Timer();

    private double mDuration, mSpeed;

    public AutoBalanceAction() {

    }

    @Override
    public boolean isFinished() {
        return Math.abs(mSwerve.getBeans()) > 20;
    }

    @Override
    public void start() {
        mSwerve.emptyBucket();
        mSwerve.drive(0.2, 0, 0, true);
        System.out.println("[Auto] Action '" + getClass().getSimpleName() + "' started!");
    }

    @Override
    public void update() {
    }

    @Override
    public void done() {
        mSwerve.stop();
        System.out.println("[Auto] Action '" + getClass().getSimpleName() + "' finished!");
    }
}

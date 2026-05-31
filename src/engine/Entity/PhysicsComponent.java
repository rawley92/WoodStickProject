package engine.Entity;

public class PhysicsComponent {

    public double x;
    public double y;

    public double velX;
    public double velY;
    
    public double accX = 0;
    public double accY = 0;


    public double radius = 0.3;

    public void stop() {
        velX = 0;
        velY = 0;
    }
}
package nova.library.graphics.novagl;

import nova.library.core.Engine;
import nova.library.vecmath.Vector3d;

public class NGLCamera
{
	public static int TYPE_FLYBY = 0;
	public static int TYPE_RTS = 1;
	public static int TYPE_FPS = 2;	
	
	public static final Vector3d forward = new Vector3d(0,0,-1);
	public static final Vector3d back = new Vector3d(0,0,1);
	public static final Vector3d left = new Vector3d(-1,0,0);
	public static final Vector3d right = new Vector3d(1,0,0);
	
	private Engine engine;
	
	public int type;
	public double x;
	public double y;
	public double z;
	public double roll;
	public double pitch;
	public double heading;
	public int velocity;
	public double angle;
	public double aspect;
	public double near;
	public double far;
	public boolean stable;
	public int cell_x;
	public int cell_z;
	
	
	public NGLCamera(Engine engine,int type)
	{
		this.type = type;
		this.engine = engine;
		if(type==TYPE_RTS)
		{
			x = 1000.0;
			y = 2000.0;
			z = 1000.0;
			roll = 0.0;
			pitch = 45.0;
			heading = 45.0;
			angle = 45.0;
			aspect = 1.0;
			near = 1.0;
			far = 100000.0;
			velocity = 0;
			stable = false;
		}
		else if(type==TYPE_FPS)
		{
			x = 05;
			y = 80;//100.0;//1.8288;//6 foot
			z = 05;
			/*x = 593;
			y = 73;//100.0;//1.8288;//6 foot
			z = 1190;
			
			 x = 497;
			y = 40;//100.0;//1.8288;//6 foot
			z = 469;
			 */
			roll = 0.0;
			pitch = 11.0;
			heading = 163.0;
			angle = 45.0;
			aspect = 1.0;
			near = 1.0;
			far = 100000.0;
			velocity = 5;
			stable = false;
		}
		else if(type==TYPE_FLYBY)
		{
			x = 50.0;
			y = 100.0;
			z = 50.0;
			roll = 0.0;
			pitch = 90.0;
			heading = 0.0;
			angle = 45.0;
			aspect = 1.0;
			near = 1.0;
			far = 100000.0;
			velocity = 0;
			stable = false;
		}
	}
	
	public void move(final Vector3d dir)
	{
		move(dir,velocity);
	}
	public void move(final Vector3d dir,int velocity)
	{
		move(dir,velocity,0);
	}
	public void move(final Vector3d dir,int velocity,int limit)
	{
		//Adjust X,Z
		Vector3d vec = new Vector3d(dir);
		if(type==NGLCamera.TYPE_RTS)
		{
			//vec.rotX(-pitch);
			//dir.rotZ(-roll);
			vec.rotY(-heading);
		}
		else if(type==NGLCamera.TYPE_FLYBY)
		{
			vec.rotX(-pitch);
			dir.rotZ(-roll);
		}
		else if(type==NGLCamera.TYPE_FPS)
		{
			vec.rotY(-heading);
		}
			
		vec.normalize();
		vec.mul(velocity);
		
		x += vec.x;
		y += vec.y;
		z += vec.z;
		if(limit!=-1)
		{
			x = Math.min(Math.max(x,limit),engine.terrain.cell_size*engine.terrain.num_cols);
			z = Math.min(Math.max(z,limit),engine.terrain.cell_size*engine.terrain.num_rows);
		}
		cell_x = (int)(x/engine.terrain.cell_size);
		cell_z = (int)(z/engine.terrain.cell_size);
		
		//Adjust Y
		if(type==NGLCamera.TYPE_FPS)
		{
			double h = (engine.terrain.verticies[cell_z][cell_x]+engine.terrain.verticies[cell_z][cell_x+1]+engine.terrain.verticies[cell_z+1][cell_x]+engine.terrain.verticies[cell_z+1][cell_x+1])/4;
			y = h+1.9;
		}
		else if(type==NGLCamera.TYPE_RTS)
		{
			int cell_x_limited = Math.min(Math.max(cell_x,0),engine.terrain.num_cols-3);
			int cell_z_limited = Math.min(Math.max(cell_z,0),engine.terrain.num_rows-3);
			
			double h = (engine.terrain.verticies[cell_z_limited][cell_x_limited]+engine.terrain.verticies[cell_z_limited][cell_x_limited+1]+engine.terrain.verticies[cell_z_limited+1][cell_x_limited]+engine.terrain.verticies[cell_z_limited+1][cell_x_limited+1])/4;
			y = Math.max(h+1.9,y);
		}
	}
}

package nova.library.graphics.vecmath;

import javax.vecmath.Matrix4f;
 
public class Vector3d extends javax.vecmath.Vector3d
{
	public static final Vector3d NORTH = new Vector3d(1,0.0,0);
	public static final Vector3d SOUTH = new Vector3d(-1,0.0,0);
	public static final Vector3d EAST =  new Vector3d(0,0.0,1);
	public static final Vector3d WEST =  new Vector3d(0,0.0,-1);
	
	public Vector3d(double x,double y,double z)
	{
		super(x,y,z);
	}
	public Vector3d(Vector3d vec)
	{
		super(vec);
	}
	public Vector3d()
	{
		super();
	}
	public static Vector3d normalize(Vector3d a)
	{
		double m = Math.sqrt((a.x*a.x)+(a.y*a.y)+(a.z*a.z));
		return new Vector3d(a.x/m,a.y/m,a.z/m);
	}
	public static Vector3d cross(Vector3d a,Vector3d b)
	{
		return new Vector3d(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);
	}
	public static double dot(Vector3d a,Vector3d b)
	{
		return a.x*b.x+a.y*b.y+a.z*b.z;
	}
	public static Vector3d subtract(Vector3d a,Vector3d b)
	{
		return new Vector3d(a.x-b.x,a.y-b.y,a.z-b.z);
	}
	public void rotX(double angle)
	{
		double cp = Math.cos(angle*Math.PI/180.0);
		double sp = Math.sin(angle*Math.PI/180.0);
		double matrix[][] = {{1,0,0,0},{0,cp,sp,0},{0,-sp,cp,0},{0,0,0,1}};
				
		double sum;
		double a[] = {x,y,z,0.0};
		double aa[] = new double[4];		 
		for(int i = 0 ; i < 4 ; i++)
		{
			sum = 0.0;
			for(int j = 0 ; j < 4 ; j++)
			{
				sum += a[j]*matrix[j][i];
			}
			aa[i] = sum;
		}
		
		x = aa[0];
		y = aa[1];
		z = aa[2];
	}
	public void rotY(double angle)
	{
		double cp = Math.cos(angle*Math.PI/180.0);
		double sp = Math.sin(angle*Math.PI/180.0);
		double matrix[][] = {{cp,0,-sp,0},{0,1,0,0},{sp,0,cp,0},{0,0,0,1}};
		
		double sum;
		double a[] = {x,y,z,0.0};
		double aa[] = new double[4];		 
		for(int i = 0 ; i < 4 ; i++)
		{
			sum = 0.0;
			for(int j = 0 ; j < 4 ; j++)
			{
				sum += a[j]*matrix[j][i];
			}
			aa[i] = sum;
		}
		
		x = aa[0];
		y = aa[1];
		z = aa[2];
		
	}
	public void rotZ(double angle)
	{
		double cp = Math.cos(angle*Math.PI/180.0);
		double sp = Math.sin(angle*Math.PI/180.0);
		double matrix[][] = {{cp,sp,0,0},{-sp,cp,0,0},{0,0,1,0},{0,0,0,1}};
		
		double sum;
		double a[] = {x,y,z,0.0};
		double aa[] = new double[4];		 
		for(int i = 0 ; i < 4 ; i++)
		{
			sum = 0.0;
			for(int j = 0 ; j < 4 ; j++)
			{
				sum += a[j]*matrix[j][i];
			}
			aa[i] = sum;
		}
		
		x = aa[0];
		y = aa[1];
		z = aa[2];
	}
	public void mul(double scalar)
	{
		x *= scalar;
		y *= scalar;
		z *= scalar;
	}
}

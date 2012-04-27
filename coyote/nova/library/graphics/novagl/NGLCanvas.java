package nova.library.graphics.novagl;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.script.ScriptException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Point3d;

import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import nova.game.Coyote;
import nova.library.settings.Settings;
import nova.library.utilities.Parser;
import nova.library.graphics.vecmath.Face3d;
import nova.library.graphics.vecmath.Vector3d;
import nova.library.graphics.vecmath.Vertex3d;

public class NGLCanvas extends JPanel
	implements MouseListener, MouseMotionListener, MouseWheelListener, FocusListener, GLEventListener, KeyListener, ComponentListener
{
	public static final int DISPLAY_LIST_TERRAIN = 1;
	
	public static final int DISPLAY_RL_TIME = 0;
	public static final int DISPLAY_GAME_TIME = 1;
	public static final int DISPLAY_TIME_PLAYED = 2;
	
	public static final float COLOR_WHITE[] = {1f,1f,1f,1.0f};
	public static final float COLOR_RED[] = {1f,0f,0f,1.0f};
	public static final float COLOR_ORANGE[] = {1f,.55f,0f,1.0f};
	public static final float COLOR_GREEN[] = {0f,1f,0f,1.0f};
	public static final float COLOR_BLUE[] = {0f,0f,1f,1.0f};
	public static final float COLOR_BLACK[] = {0f,0f,0f,1.0f};
	
	private static final int HUD_STATE_NORMAL = 0;
	private static final int HUD_STATE_SELECTED = 1;
	private static final int HUD_STATE_HOVER = 2;
	
	private int width;
	private int height;
	
	public Image reticle;
	public boolean mouselook;
	public boolean mouselook_locked;
	public boolean nav_forward,nav_strafe_left,nav_backward,nav_strafe_right,nav_turn_left,nav_turn_right,nav_pan_up,nav_pan_down,nav_pan_left,nav_pan_right;
	
	public boolean mouse_1,mouse_2;
	public int mouse_x,mouse_y;
	
	private int currentCamera = -1;
	private NGLCamera cameras[];
	private Coyote coyote;
	
	private HUD hud;
	
	//------So encapsulate GLJPanel/GLCanvas with the NGLCanvas and whenever it is activated createa new one
	//---Whenever it is deactivated destroy it by setting canvas to null and disposing if you have too
	//---Encapsualte repaint too bypass repainting this and just send repaint to the canvas field somehow (maybe call a custom function on ) NGL
	//---Canvas instead of calling nglcanvas.repaint() call nglcanvas.repaint_canvas(){canvas.repaint()}
	private GLCanvas canvas;

	//Derived from rayselection of the terrain
	private NGLTerrainSelection selection; 
	
	public NGLCanvas(Coyote coyote,int width,int height)
	{
		this.coyote = coyote;
		this.width = width;
		this.height = height;
		//this.setPreferredSize(new Dimension(width,height));
		
		Engine engine = game.getEngine();
		cameras = new NGLCamera[]{new NGLCamera(engine,NGLCamera.TYPE_FLYBY),new NGLCamera(engine,NGLCamera.TYPE_RTS),new NGLCamera(engine,NGLCamera.TYPE_FPS)};
		hud = new HUD(coyote);
		
		this.setLayout(new GridLayout(1,1));
		this.setBackground(Color.BLACK);
		this.addComponentListener(this);	
		
		int[] pixels = new int[16*16];
		for(int y = 0; y < 16; y++)
		{
			for(int x = 0; x < 16; x++)
			{
				if(true)
				{
					pixels[y*16+x] = 0xAAAAAAA;
				}
			}
		}
		reticle = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16,16,pixels,0,16));
	}

	public void activate()
	{		
		Engine engine = coyote.getEngine();
		Settings settings = coyote.getSettings();
		//Dimension res = Parser.parseDimension(settings.getString(Settings.RESOLUTION));
		
		//Set Current Camera based on currentMode
		currentCamera = engine.currentMode.camera;
		hud.activate(width,height);
		this.setVisible(true);
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp); 
		//canvas = new GLCanvas(caps);
		canvas = new GLCanvas();
		
		//canvas.setBounds(0,0,this.getWidth(),this.getHeight());
		
		mouselook = engine.currentMode.defaultMouselook;
		canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
				
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addFocusListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addMouseWheelListener(this);
		this.add(canvas);
		this.requestFocus();
		repaintCanvas();
		this.revalidate();
		
		//selection = new NGLTerrainSelection(NGLTerrainSelection.MODE_DRAG_RECTANGLE,game.getEngine().terrain);
		selection = new NGLTerrainSelection(NGLTerrainSelection.MODE_DRAG_LINE,engine.terrain);
	}

	public void deactivate()
	{
		if(canvas!=null)
		{
			//this.setVisible(false);
			this.remove(canvas);
			canvas.removeGLEventListener(this);
			canvas.removeKeyListener(this);
			canvas.removeFocusListener(this);
			canvas.removeMouseListener(this);
			canvas.removeMouseMotionListener(this);
			canvas.removeMouseWheelListener(this);
			
			nav_forward = false;
			nav_strafe_left = false;
			nav_backward = false;
			nav_strafe_right = false;
			
			nav_pan_left = false;
			nav_pan_right = false;
			nav_pan_up = false;
			nav_pan_down = false;
			
			mouse_x = 0;
			mouse_y = 0;
			
			canvas = null;
			//this.revalidate();
			
			selection = null;
		}
	}

	public void repaintCanvas()
	{
		this.canvas.display();
	}
	
	
	public NGLCamera getActiveCamera()
	{
		return cameras[currentCamera];
	}
	
	/**
	 * Adjusts Camera based on user input
	 */
	public void updateSelection()
	{
		if(mouse_1)
		{
			selection.end();
			if(selection.mode==NGLTerrainSelection.MODE_DRAG_LINE||selection.mode==NGLTerrainSelection.MODE_DRAG_RECTANGLE)
			{
				selection.calculate();
			}
		}	
	}
	public void navigate(Controller controller,double mouse_sensitivity)
	{
		Engine engine = coyote.getEngine();
		KeyBindings bindings = coyote.getSettings().getKeyBindings();
		NGLCamera camera = getActiveCamera();
		
		if(camera.type==NGLCamera.TYPE_RTS)
		{
			
			
			/*if(nav_pan_up&&!nav_pan_down) camera.z -= 10.0*(((int)camera.y)/100);
			if(!nav_pan_up&&nav_pan_down) camera.z += 10.0*(((int)camera.y)/100);
			if(nav_pan_left&&!nav_pan_right) camera.x -= 10.0*(((int)camera.y)/100);
			if(!nav_pan_left&&nav_pan_right) camera.x += 10.0*(((int)camera.y)/100);
			*/
			controller.poll();
			if(mouselook)
			{
				EventQueue queue = controller.getEventQueue();
				for(Event event = new Event();queue.getNextEvent(event);)
				{
					if(event.getComponent().getName().equals("x"))
					{
						camera.heading += mouse_sensitivity*event.getValue();
						if(camera.heading>360.0) camera.heading -= 360;
						else if(camera.heading<0) camera.heading += 360;
					}
					else if(event.getComponent().getName().equals("y"))
					{
						camera.pitch = Math.min(90.0,Math.max(-90.0,camera.pitch+mouse_sensitivity*event.getValue()));
					}
				}
			}
			
			if(nav_pan_up&&!nav_pan_down) camera.move(NGLCamera.forward,10*(((int)camera.y)/100),-200);
			else if(!nav_pan_up&&nav_pan_down) camera.move(NGLCamera.back,10*(((int)camera.y)/100),-200);
			
			//Strafe
			if(nav_pan_left&&!nav_pan_right) camera.move(NGLCamera.left,10*(((int)camera.y)/100),-200);
			if(!nav_pan_left&&nav_pan_right) camera.move(NGLCamera.right,10*(((int)camera.y)/100),-200);
			/*
			
			
			if(nav_pan_up&&!nav_pan_down) camera.move(new Vector3d(1,0,-1),10*(((int)camera.y)/100),true);
			if(!nav_pan_up&&nav_pan_down) camera.move(new Vector3d(-1,0,1),10*(((int)camera.y)/100),true);
			if(nav_pan_left&&!nav_pan_right) camera.move(new Vector3d(-1,0,-1),10*(((int)camera.y)/100),true);
			if(!nav_pan_left&&nav_pan_right) camera.move(new Vector3d(1,0,1),10*(((int)camera.y)/100),true);
			*/
		}
		else if(camera.type==NGLCamera.TYPE_FLYBY)
		{
			if(nav_forward&&!nav_backward) camera.velocity++;
			else if(!nav_forward&&nav_backward) camera.velocity--;
			
			if(!camera.stable)
			{
				camera.pitch += (-1.0*mouse_y)/(height/2.0);
				camera.heading += (-1.0*mouse_x)/(width/2.0);			
			}
			camera.move(NGLCamera.forward);
		}
		else if(camera.type==NGLCamera.TYPE_FPS)
		{
			controller.poll();
			if(mouselook)
			{
				EventQueue queue = controller.getEventQueue();
				for(Event event = new Event();queue.getNextEvent(event);)
				{
					if(event.getComponent().getName().equals("x"))
					{
						camera.heading += mouse_sensitivity*event.getValue();
						if(camera.heading>360.0) camera.heading -= 360;
						else if(camera.heading<0) camera.heading += 360;
					}
					else if(event.getComponent().getName().equals("y"))
					{
						camera.pitch = Math.min(90.0,Math.max(-90.0,camera.pitch+mouse_sensitivity*event.getValue()));
					}
				}
			}
			
			//Forward and Back
			if(nav_forward&&!nav_backward)camera.move(NGLCamera.forward);
			else if(!nav_forward&&nav_backward)camera.move(NGLCamera.back);
			//Turning
			if(nav_turn_left&&!nav_turn_right) camera.heading -= 2;
			else if(!nav_turn_left&&nav_turn_right) camera.heading += 2;

			//Strafe
			if(nav_strafe_left&&!nav_strafe_right)camera.move(NGLCamera.left);
			else if(!nav_strafe_left&&nav_strafe_right)camera.move(NGLCamera.right);
		}
	}
	/**
	 * Adjust Camera based on computer input such as deciding to follow
	 */
	public void modifyActiveCamera()
	{
		
	}
	
	
	public void display(GLAutoDrawable drawable)
	{
		
		Engine engine = coyote.getEngine();
		GL2 gl = drawable.getGL().getGL2();
		GLU glu = new GLU();

		//float lineWidths[] = new float[10];
		//gl.glGetFloatv(GL2.GL_LINE_WIDTH_RANGE,lineWidths,2);
		//System.err.println("Line Width Range: "+lineWidths[0]+" - "+lineWidths[1]);
		
		gl.glClearColor(1.0f,1.0f,1.0f,1.0f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
		if(engine.loaded)
		{			
			display_3D(gl);
			display_2D(gl);
			
	        /*
	        gl.glMatrixMode(GL.GL_PROJECTION);
	        gl.glLoadIdentity();
	        gl.glOrtho(left, right, bottom, top, znear, zfar);
	        gl.glMatrixMode(GL.GL_MODELVIEW);
	        gl.glLoadIdentity();
	        gl.glDisable(GL.GL_DEPTH_TEST);
	        display_hud(gl);
	        
	      	        
	        render3Dview();
	        glPushMatrix();
	          glMatrixMode(GL_MODELVIEW);
	          glLoadIdentity();
	          glMatrixMode(GL_PROJECTION);
	          glPushMatrix();
	            glLoadIdentity();
	            gluOrtho2D(0, 1, 0, 1);
	            render2Doverlay();
	          glPopMatrix();
	        glPopMatrix();
	        glMatrixMode(GL_MODELVIEW);
	        */
		}
        gl.glFlush();
	}
		
	private void display_3D(GL2 gl)
	{
		NGLCamera c = cameras[currentCamera];
		gl.glLoadIdentity();
		gl.glRotatef((float)c.roll,0.0f,0.0f,1.0f);
        gl.glRotatef((float)c.pitch,1.0f,0.0f,0.0f);
        gl.glRotatef((float)c.heading,0.0f,1.0f,0.0f);
        gl.glTranslated(-c.x,-c.y,-c.z);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        
		gl.glEnable(GL2.GL_LIGHTING);
		Engine engine = coyote.getEngine();
		raytracing_triangles(gl,engine.terrain);
		display_terrain(gl,engine.terrain);
        //display_lots(gl);
	}
	public void raytracing_circles(GL2 gl,NGLTerrain terrain)
	{
		//selection.clearBuffer();
		NGLCamera c = cameras[currentCamera];
		Vector3d r = new Vector3d(0,0,-1);
		r.rotX(-c.pitch);
		r.rotY(-c.heading);
		//r.rotZ(c.roll);
		r.normalize();
		double currentDistance = -1;
		double d = 0;
		double max = Math.sqrt(((double)terrain.cell_size)*((double)terrain.cell_size)/2.0);		
		
		for(int z = 0; z < terrain.landcover.length; z++)
		{
			for(int x = 0; x < terrain.landcover[0].length; x++)
			{				
				//t = (B-A).n / d.n 
				double px = (x+0.5)*terrain.cell_size;
				double py = terrain.heightmap[z+1][x+1];
				double pz = (z+0.5)*terrain.cell_size;
				double numerator = (px-c.x)*terrain.planes[z][x].x+(py-c.y)*terrain.planes[z][x].y+(pz-c.z)*terrain.planes[z][x].z;
				double denominator = r.x*terrain.planes[z][x].x+r.y*terrain.planes[z][x].y+r.z*terrain.planes[z][x].z;
				if(denominator!=0)
				{
					d = numerator/denominator;
					if((d>0)&&(currentDistance==-1||d>(-1.0*currentDistance)))//Positive distance (/viewable) and less than current distance
					{
						if(Math.sqrt(((c.x+d*r.x)-px)*((c.x+d*r.x)-px)+((c.y+d*r.y)-py)*((c.y+d*r.y)-py)+((c.z+d*r.z)-pz)*((c.z+d*r.z)-pz))<max)//Within max distance
						{
							//System.err.println("Selected Cell: "+x+","+z);
							//System.err.println("\tDistance: "+d);
							//System.err.println("Distance From Heightmap Point to Interesection of Ray and Plane: "+(Math.sqrt(((c.x+d*r.x)-px)*((c.x+d*r.x)-px)+((c.y+d*r.y)-py)*((c.y+d*r.y)-py)+((c.z+d*r.z)-pz)*((c.z+d*r.z)-pz))));
							currentDistance = d;
							selection.setCurrent(d,x,z);
						}
					}
				}
			}
		}
	}
	public void raytracing_triangles(GL2 gl,NGLTerrain terrain)
	{
		//selection.clearBuffer();
		NGLCamera c = cameras[currentCamera];
		Vector3d r = new Vector3d(0,0,-1);
		r.rotX(-c.pitch);
		r.rotY(-c.heading);
		//r.rotZ(c.roll);
		r.normalize();
		double currentDistance = -1;
		double d = 0;
		double max = Math.sqrt(((double)terrain.cell_size)*((double)terrain.cell_size)/2.0);		
		
		for(int z = 0; z < terrain.landcover.length; z++)
		{
			for(int x = 0; x < terrain.landcover[0].length; x++)
			{				
				for(int n = 0; n < 2; n++)//For Both Triangles in this cell
				{
					
					//..........
					//Broken
					//http://en.wikipedia.org/wiki/Barycentric_coordinate_system_%28mathematics%29
					//Don't use terrain.planes but the precalculated tiranlgle planes
					
					//t = (B-A).n / d.n 
					double px = (x+0.5)*terrain.cell_size;
					double py = terrain.verticies[z][x];
					double pz = (z+0.5)*terrain.cell_size;
					//(p0 - L0 ) * Plane Normal
					double numerator = (px-c.x)*terrain.normals_triangles[z][x][n].x+(py-c.y)*terrain.normals_triangles[z][x][n].y+(pz-c.z)*terrain.normals_triangles[z][x][n].z;
					//Camera Ray * Plane Normal
					double denominator = r.x*terrain.normals_triangles[z][x][n].x+r.y*terrain.normals_triangles[z][x][n].y+r.z*terrain.normals_triangles[z][x][n].z;	
					if(denominator!=0)
					{
						d = numerator/denominator;
						if((d>0)&&(currentDistance==-1||d>(-1.0*currentDistance)))//Positive distance (/viewable) and less than current distance
						{
							if(Math.sqrt(((c.x+d*r.x)-px)*((c.x+d*r.x)-px)+((c.y+d*r.y)-py)*((c.y+d*r.y)-py)+((c.z+d*r.z)-pz)*((c.z+d*r.z)-pz))<max)//Within max distance
							{
								//currentDistance = d;
								//selection.setCurrent(d,x,z);
								
								//Barycentric math to test if in correct triangle
								//http://en.wikipedia.org/wiki/Barycentric_coordinate_system_%28mathematics%29
								//double det = (2*terrain.cell_size*terrain.cell_size);
								double det = (n==0?-1.0:1.0)*terrain.cell_size*terrain.cell_size;
								//double lambda_1 = (terrain.cell_size*((c.x+d*r.x)-((x+1)*terrain.cell_size))+terrain.cell_size*((c.z+d*r.z)-((z+1)*terrain.cell_size)))/det;
								double lambda_1 = terrain.cell_size*((n==0)?((c.z+d*r.z)-((z+1)*terrain.cell_size)):(-1.0*((c.x+d*r.x)-((x+1)*terrain.cell_size))))/det;
								//if (n==0)
///									lambda_1 = ((terrain.cell_size*((c.z+d*r.z)-((z+1)*terrain.cell_size)))/det);
								//else
//									lambda_1 = ((-1.0*terrain.cell_size*((c.x+d*r.x)-((x+1)*terrain.cell_size)))/det);
								double lambda_2 = (terrain.cell_size*((c.x+d*r.x)-((x+1)*terrain.cell_size))+(-1.0*terrain.cell_size*((c.z+d*r.z)-((z+1)*terrain.cell_size))))/det;
								double lambda_3 = 1 - lambda_1 - lambda_2;
								//System.err.println("Lambda 1: "+lambda_1);
								//System.err.println("Lambda 2: "+lambda_2);
								//System.err.println("Lambda 3: "+lambda_3);
								//double lambda_2 == lambda_1;
								//double lambda_2 = (terrain.cell_size*(tx-x3)+terrain.cell_size*(ty-y3))/det;
								//double lambda_1 = (((y2-y3)*(tx-x3))+((x3-x2)*(ty-y3)))/(((y2-y3)*(x1-x3))+((x3-x2)*(y1-y3)));
								//double lambda_1 = ((terrain.cell_size*(tx-x3))+(terrain.cell_size*(ty-y3)))/((terrain.cell_size*terrain.cell_size)+(terrain.cell_size*terrain.cell_size));
								//double lambda_2 = ((terrain.cell_size*(tx-x3))+(terrain.cell_size*(ty-y3)))/((terrain.cell_size*terrain.cell_size)+(terrain.cell_size*terrain.cell_size));;
								
								if(lambda_1>=0&&lambda_2>=0&&lambda_3>=0&&lambda_1<=1&&lambda_2<=1&&lambda_3<=1)
								{
									//System.err.println("Selected Cell: "+x+","+z);
									//System.err.println("\tDistance: "+d);
									//System.err.println("Distance From Heightmap Point to Interesection of Ray and Plane: "+(Math.sqrt(((c.x+d*r.x)-px)*((c.x+d*r.x)-px)+((c.y+d*r.y)-py)*((c.y+d*r.y)-py)+((c.z+d*r.z)-pz)*((c.z+d*r.z)-pz))));
									currentDistance = d;
									selection.setCurrent(d,x,z);
								}
							}
						}
					}
				}
			}
		}
		//System.err.println("------------------------------------------------------");
	}
	
	private void old_display_terrain(GL2 gl,NGLTerrain terrain)
	{
		//if(!engine.terrain.compiled)
//			engine.terrain.compile(DISPLAY_LIST_TERRAIN,gl);
		//gl.glCallList(DISPLAY_LIST_TERRAIN);
		//engine.terrain.display(gl);
		
	
		//System.err.println("-------------------------------------------------");
		//System.err.println("GL_LIGHTING: "+gl.glIsEnabled(GL2.GL_LIGHTING));
		//System.err.println("GL_LIGHT0: "+gl.glIsEnabled(GL2.GL_LIGHT0));
		//System.err.println("GL_LIGHT1: "+gl.glIsEnabled(GL2.GL_LIGHT1));
		//System.err.println("GL_DEPTH_TEST: "+gl.glIsEnabled(GL2.GL_DEPTH_TEST));
		//float params[] = new float[4];
		//gl.glGetLightfv(GL2.GL_LIGHT0,GL2.GL_POSITION, params,0);
		//System.err.print("GL_LIGHT0 Params:");
		//for(int p = 0; p < params.length; p++)
		//	System.err.print(" "+params[p]);
		//System.err.println();
		//System.err.println("Num Verticies: "+terrain.verticies.length+","+terrain.verticies[0].length);
		//System.err.println("Number of Normals: "+terrain.normals.length+","+terrain.normals[0].length);
		//System.err.println("Number of Textures: "+terrain.textures.length+","+terrain.textures[0].length);
		/*------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *THE PROBLEM IS WITH THE TEXTURES BEING ENABLED, AFTER THEY ARE ENABLED THE LIGHTING DOESN'T BLEND SHADE THE QUADS CORRECTLY
		 *THAT'S WHY THE GRID LINES ARE REFLECTING THE LIGHT'S COLOR B/C 
		 *SEE: "Why doesn't lighting work when I turn on texture mapping?" in http://www.opengl.org/resources/faq/technical/texture.htm
		 *gl.glDisable(GL2.GL_TEXTURE_2D);
		 *
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * gl.glMaterialfv MUST OCCUR BEFORE GL.GLBEGIN
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * System.err.println("Selected Cell: "+selection.x+","+selection.z);
		System.err.println("\tDistance: "+selection.d);
			//gl.glDisable(GL2.GL_TEXTURE_2D);
		 */
		Engine engine = coyote.getEngine();
		NGLCamera c = cameras[currentCamera];
		int current_texture = engine.landcover.get(terrain.landcover[0][0]).texture;
		gl.glPolygonMode(GL.GL_FRONT,GL2.GL_FILL);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL2.GL_MODULATE);//GL.GL_REPLACE
		engine.textures.get(current_texture).texture.bind(gl);
		engine.textures.get(current_texture).texture.enable(gl);
		
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
		gl.glBegin(GL2.GL_TRIANGLES);
		for(int z = 0; z < terrain.landcover.length; z++)
		{
			for(int x = 0; x < terrain.landcover[0].length; x++)
			{
				//System.err.println("NGL Canvas (Z,X): "+z+","+x);
				if(current_texture!=engine.landcover.get(terrain.landcover[0][0]).texture)
				{
					current_texture = engine.landcover.get(terrain.landcover[0][0]).texture;
					gl.glEnd();
					engine.textures.get(current_texture).texture.bind(gl);
					engine.textures.get(current_texture).texture.enable(gl);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				BalboaTexture texture = engine.textures.get(current_texture);
				if(selection.isCurrent(x,z))
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_ORANGE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_ORANGE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals_triangles[z][x][0].x,(float)terrain.normals_triangles[z][x][0].y,(float)terrain.normals_triangles[z][x][0].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						//Vertex 2
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.left,texture.top);
						gl.glNormal3f((float)terrain.normals_triangles[z+1][x][0].x,(float)terrain.normals_triangles[z+1][x][0].y,(float)terrain.normals_triangles[z+1][x][0].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals_triangles[z+1][x+1][0].x,(float)terrain.normals_triangles[z+1][x+1][0].y,(float)terrain.normals_triangles[z+1][x+1][0].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals_triangles[z+1][x+1][1].x,(float)terrain.normals_triangles[z+1][x+1][1].y,(float)terrain.normals_triangles[z+1][x+1][1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 4
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.bottom);
						gl.glNormal3f((float)terrain.normals_triangles[z][x+1][1].x,(float)terrain.normals_triangles[z][x+1][1].y,(float)terrain.normals_triangles[z][x+1][1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals_triangles[z][x][1].x,(float)terrain.normals_triangles[z][x][1].y,(float)terrain.normals_triangles[z][x][1].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						
					}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				else if(selection.isSelected(x,z))
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_RED,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_RED,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						//Vertex 2
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.left,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 4
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						
					}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				else if(z==c.cell_z&&x==c.cell_x)
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_BLUE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_BLUE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						//Vertex 2
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.left,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 4
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						
					}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}					
				else
				{
					gl.glBegin(GL2.GL_TRIANGLES);
					try{
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
						//Vertex 2
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.left,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 3
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.top);
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
						//Vertex 4
						gl.glColor3f(.5f,.5f,.5f);
						gl.glTexCoord2f(texture.right,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
						//Vertex 1
						gl.glColor3f(.5f,.5f,.5f);
					    gl.glTexCoord2f(texture.left,texture.bottom);
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
					}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
				}
			}
		}
		gl.glEnd();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		/*
		if(settings.getBoolean(Settings.DISPLAY_GRID))
		{
			//gl.glColor4f(1.0f,1.0f,0.0f,1.0f);
			if(((int)engine.currentMode.offsetGrid)==0)
			{
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK,GL2.GL_LINE);
				gl.glLineWidth(Float.parseFloat(settings.getString(Settings.GRID_WIDTH)));
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,settings.getColor(Settings.GRID_COLOR).getColorComponents(new float[3]),0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{1.0f,1.0f,1.0f,1.0f},0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{1f},0);
				for(int z = 0; z < terrain.textures.length; z++)
				{
					gl.glBegin(GL2.GL_QUADS);
					for(int x = 0; x < terrain.textures[0].length; x++)
					{
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
					}
					gl.glEnd();
				}
			}
			else
			{
				double m = engine.currentMode.offsetGrid;
				Vector3d n;
				Vector3d n2;
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK,GL2.GL_LINE);
				gl.glLineWidth(Float.parseFloat(settings.getString(Settings.GRID_WIDTH)));
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,settings.getColor(Settings.GRID_COLOR).getColorComponents(new float[3]),0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{0f,0f,0f,0f},0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{0f},0);
				for(int z = 0; z < terrain.textures.length; z++)
				{
					for(int x = 0; x < terrain.textures[0].length; x++)
					{
						gl.glBegin(GL2.GL_LINE_LOOP);
						n = terrain.normals[z][x];
						n2 = Vector3d.normalize(n);
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)(terrain.verticies[z][x]+m*n2.y),(float)(z*terrain.cell_size));
						
						n = terrain.normals[z+1][x];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)(terrain.verticies[z+1][x]+m*n2.y),(float)((z+1)*terrain.cell_size));
						
						n = terrain.normals[z+1][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)(terrain.verticies[z+1][x+1]+m*n2.y),(float)((z+1)*terrain.cell_size));
						
						n = terrain.normals[z][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)(terrain.verticies[z][x+1]+m*n2.y),(float)(z*terrain.cell_size));

						/*
						 *
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z][x]+m*n2.y),(float)(z*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z+1][x];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z+1][x]+m*n2.y),(float)((z+1)*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z+1][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z+1][x+1]+m*n2.y),(float)((z+1)*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z][x+1]+m*n2.y),(float)(z*terrain.cell_size+m*n2.z));
						 
						
						gl.glEnd();
						///////////////////////////////////
						
					/*
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
					}
				}
			}	
		}*/
	}
	private void display_terrain(GL2 gl,NGLTerrain terrain)
	{
		//if(!engine.terrain.compiled)
//			engine.terrain.compile(DISPLAY_LIST_TERRAIN,gl);
		//gl.glCallList(DISPLAY_LIST_TERRAIN);
		//engine.terrain.display(gl);
		
	
		//System.err.println("-------------------------------------------------");
		//System.err.println("GL_LIGHTING: "+gl.glIsEnabled(GL2.GL_LIGHTING));
		//System.err.println("GL_LIGHT0: "+gl.glIsEnabled(GL2.GL_LIGHT0));
		//System.err.println("GL_LIGHT1: "+gl.glIsEnabled(GL2.GL_LIGHT1));
		//System.err.println("GL_DEPTH_TEST: "+gl.glIsEnabled(GL2.GL_DEPTH_TEST));
		//float params[] = new float[4];
		//gl.glGetLightfv(GL2.GL_LIGHT0,GL2.GL_POSITION, params,0);
		//System.err.print("GL_LIGHT0 Params:");
		//for(int p = 0; p < params.length; p++)
		//	System.err.print(" "+params[p]);
		//System.err.println();
		//System.err.println("Num Verticies: "+terrain.verticies.length+","+terrain.verticies[0].length);
		//System.err.println("Number of Normals: "+terrain.normals.length+","+terrain.normals[0].length);
		//System.err.println("Number of Textures: "+terrain.textures.length+","+terrain.textures[0].length);
		/*------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *------------------------------------------------------------------------------------------------------------------------------------------------------
		 *THE PROBLEM IS WITH THE TEXTURES BEING ENABLED, AFTER THEY ARE ENABLED THE LIGHTING DOESN'T BLEND SHADE THE QUADS CORRECTLY
		 *THAT'S WHY THE GRID LINES ARE REFLECTING THE LIGHT'S COLOR B/C 
		 *SEE: "Why doesn't lighting work when I turn on texture mapping?" in http://www.opengl.org/resources/faq/technical/texture.htm
		 *gl.glDisable(GL2.GL_TEXTURE_2D);
		 *
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * gl.glMaterialfv MUST OCCUR BEFORE GL.GLBEGIN
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * ------------------------------------------------------------------------------------------------------------------------------------------------------
		 * System.err.println("Selected Cell: "+selection.x+","+selection.z);
		System.err.println("\tDistance: "+selection.d);
			//gl.glDisable(GL2.GL_TEXTURE_2D);
		 */
		Engine engine = coyote.getEngine();
		NGLCamera c = cameras[currentCamera];
		int current_texture = engine.landcover.get(terrain.landcover[0][0]).texture;
		gl.glPolygonMode(GL.GL_FRONT,GL2.GL_FILL);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL2.GL_MODULATE);//GL.GL_REPLACE
		engine.textures.get(current_texture).texture.bind(gl);
		engine.textures.get(current_texture).texture.enable(gl);
		
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
		gl.glBegin(GL2.GL_TRIANGLES);
		for(int z = 0; z < terrain.landcover.length; z++)
		{
			for(int x = 0; x < terrain.landcover[0].length; x++)
			{
				//System.err.println("NGL Canvas (Z,X): "+z+","+x);
				if(current_texture!=engine.landcover.get(terrain.landcover[0][0]).texture)
				{
					current_texture = engine.landcover.get(terrain.landcover[0][0]).texture;
					gl.glEnd();
					engine.textures.get(current_texture).texture.bind(gl);
					engine.textures.get(current_texture).texture.enable(gl);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				NGLTexture texture = engine.textures.get(current_texture);
				if(selection.isCurrent(x,z))
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_ORANGE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_ORANGE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{display_cell(gl,terrain,texture,z,x);}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				else if(selection.isSelected(x,z))
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_RED,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_RED,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{display_cell(gl,terrain,texture,z,x);}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}
				else if(z==c.cell_z&&x==c.cell_x)
				{
					gl.glEnd();//End Triangles
					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_BLUE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_BLUE,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,COLOR_BLACK,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
					try{display_cell(gl,terrain,texture,z,x);}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
					gl.glEnd();
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_GREEN,0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,.8f,0f,1.0f},0);
					gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{.1f},0);
					gl.glBegin(GL2.GL_TRIANGLES);
				}					
				else
				{
					gl.glBegin(GL2.GL_TRIANGLES);
					try{display_cell(gl,terrain,texture,z,x);}catch(Exception e){System.err.println("z: "+z);System.err.println("x: "+x);e.printStackTrace();System.exit(-1);};
				}
			}
		}
		gl.glEnd();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		/*
		if(settings.getBoolean(Settings.DISPLAY_GRID))
		{
			//gl.glColor4f(1.0f,1.0f,0.0f,1.0f);
			if(((int)engine.currentMode.offsetGrid)==0)
			{
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK,GL2.GL_LINE);
				gl.glLineWidth(Float.parseFloat(settings.getString(Settings.GRID_WIDTH)));
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,settings.getColor(Settings.GRID_COLOR).getColorComponents(new float[3]),0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{1.0f,1.0f,1.0f,1.0f},0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{1f},0);
				for(int z = 0; z < terrain.textures.length; z++)
				{
					gl.glBegin(GL2.GL_QUADS);
					for(int x = 0; x < terrain.textures[0].length; x++)
					{
						gl.glNormal3f((float)terrain.normals[z][x].x,(float)terrain.normals[z][x].y,(float)terrain.normals[z][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z][x],(float)(z*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
					}
					gl.glEnd();
				}
			}
			else
			{
				double m = engine.currentMode.offsetGrid;
				Vector3d n;
				Vector3d n2;
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK,GL2.GL_LINE);
				gl.glLineWidth(Float.parseFloat(settings.getString(Settings.GRID_WIDTH)));
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,settings.getColor(Settings.GRID_COLOR).getColorComponents(new float[3]),0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{0f,0f,0f,0f},0);
				gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{0f},0);
				for(int z = 0; z < terrain.textures.length; z++)
				{
					for(int x = 0; x < terrain.textures[0].length; x++)
					{
						gl.glBegin(GL2.GL_LINE_LOOP);
						n = terrain.normals[z][x];
						n2 = Vector3d.normalize(n);
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)(terrain.verticies[z][x]+m*n2.y),(float)(z*terrain.cell_size));
						
						n = terrain.normals[z+1][x];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)(terrain.verticies[z+1][x]+m*n2.y),(float)((z+1)*terrain.cell_size));
						
						n = terrain.normals[z+1][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)(terrain.verticies[z+1][x+1]+m*n2.y),(float)((z+1)*terrain.cell_size));
						
						n = terrain.normals[z][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)(terrain.verticies[z][x+1]+m*n2.y),(float)(z*terrain.cell_size));

						/*
						 *
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z][x]+m*n2.y),(float)(z*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z+1][x];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)(x*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z+1][x]+m*n2.y),(float)((z+1)*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z+1][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z+1][x+1]+m*n2.y),(float)((z+1)*terrain.cell_size+m*n2.z));
						
						n = terrain.normals[z][x+1];
						gl.glNormal3f((float)n.x,(float)n.y,(float)n.z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size+m*n2.x),(float)(terrain.verticies[z][x+1]+m*n2.y),(float)(z*terrain.cell_size+m*n2.z));
						 
						
						gl.glEnd();
						///////////////////////////////////
						
					/*
						gl.glNormal3f((float)terrain.normals[z+1][x].x,(float)terrain.normals[z+1][x].y,(float)terrain.normals[z+1][x].z);
						gl.glVertex3f((float)(x*terrain.cell_size),(float)terrain.verticies[z+1][x],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z+1][x+1].x,(float)terrain.normals[z+1][x+1].y,(float)terrain.normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z+1][x+1],(float)((z+1)*terrain.cell_size));
					
						gl.glNormal3f((float)terrain.normals[z][x+1].x,(float)terrain.normals[z][x+1].y,(float)terrain.normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*terrain.cell_size),(float)terrain.verticies[z][x+1],(float)(z*terrain.cell_size));
					}
				}
			}	
		}*/
	}
	private void display_cell(GL2 gl,NGLTerrain t, NGLTexture texture,int z,int x)
	{
		//Vertex 1
		gl.glColor3f(.5f,.5f,.5f);
	    gl.glTexCoord2f(texture.left,texture.bottom);
		gl.glNormal3f((float)t.normals[z][x].x,(float)t.normals[z][x].y,(float)t.normals[z][x].z);
		gl.glVertex3f((float)(x*t.cell_size),(float)t.verticies[z][x],(float)(z*t.cell_size));
		//Vertex 2
		gl.glColor3f(.5f,.5f,.5f);
		gl.glTexCoord2f(texture.left,texture.top);
		gl.glNormal3f((float)t.normals[z+1][x].x,(float)t.normals[z+1][x].y,(float)t.normals[z+1][x].z);
		gl.glVertex3f((float)(x*t.cell_size),(float)t.verticies[z+1][x],(float)((z+1)*t.cell_size));
		//Vertex 3
		gl.glColor3f(.5f,.5f,.5f);
		gl.glTexCoord2f(texture.right,texture.top);
		gl.glNormal3f((float)t.normals[z+1][x+1].x,(float)t.normals[z+1][x+1].y,(float)t.normals[z+1][x+1].z);
		gl.glVertex3f((float)((x+1)*t.cell_size),(float)t.verticies[z+1][x+1],(float)((z+1)*t.cell_size));
		//Vertex 3
		gl.glColor3f(.5f,.5f,.5f);
		gl.glTexCoord2f(texture.right,texture.top);
		gl.glNormal3f((float)t.normals[z+1][x+1].x,(float)t.normals[z+1][x+1].y,(float)t.normals[z+1][x+1].z);
		gl.glVertex3f((float)((x+1)*t.cell_size),(float)t.verticies[z+1][x+1],(float)((z+1)*t.cell_size));
		//Vertex 4
		gl.glColor3f(.5f,.5f,.5f);
		gl.glTexCoord2f(texture.right,texture.bottom);
		gl.glNormal3f((float)t.normals[z][x+1].x,(float)t.normals[z][x+1].y,(float)t.normals[z][x+1].z);
		gl.glVertex3f((float)((x+1)*t.cell_size),(float)t.verticies[z][x+1],(float)(z*t.cell_size));
		//Vertex 1
		gl.glColor3f(.5f,.5f,.5f);
	    gl.glTexCoord2f(texture.left,texture.bottom);
		gl.glNormal3f((float)t.normals[z][x].x,(float)t.normals[z][x].y,(float)t.normals[z][x].z);
		gl.glVertex3f((float)(x*t.cell_size),(float)t.verticies[z][x],(float)(z*t.cell_size));
	}
	private void display_lots(GL2 gl)
	{
		Engine engine = coyote.getEngine();
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK,GL2.GL_FILL);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL2.GL_REPLACE);
		for(int y = 0; y < engine.map.length; y++)
		{
			for(int x = 0; x < engine.map[0].length; x++)
			{
				if(engine.map[y][x]!=null&&engine.map[y][x].lot!=null&&engine.map[y][x].lot.building!=null)
				{
					NGLModel model = engine.map[y][x].lot.building.model;
					if(model!=null)
					{
						for(NGLGroup g: model.groups)
						{
							//System.err.println("Binding texture "+g.texture.name+" to "+model.name+"/"+g.name);
							g.texture.bind(gl);
							for(Face3d face: g.faces)
							{
								//gl.glColor4f(0f,0f,0f,1f);
								gl.glBegin(GL2.GL_POLYGON);
								for(int i = 0; i < face.verticies.length; i++)
								{
									/*
										float mat_specular[] = {1.0f,1.0f,1.0f,1.0f};
										float mat_shininess[] = {50.0f};
										float light_position[] = {0.0f,1.0f,.4f,0.0f};
										float white_light[] = {.5f,.5f,.5f,1.0f};
										float lmodel_ambient[] = {.5f,.5f,.5f,1.0f};
										float mat_amb_diff[] = {.5f,.5f,.5f,1.0f};
										gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_AMBIENT_AND_DIFFUSE,mat_amb_diff,0);
										gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_SPECULAR,mat_specular,0);
										gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_SHININESS,mat_shininess,0);
									 */
									
									gl.glTexCoord2f((float)(face.textures[i].x),(float)face.textures[i].y);
									gl.glNormal3f((float)(face.normals[i].x),(float)face.normals[i].y,(float)(face.normals[i].z));
									gl.glVertex3f((float)((x+.5)*Cell.SIZE+face.verticies[i].x),(float)(face.verticies[i].y+engine.map[y][x].h),(float)((y+.5)*Cell.SIZE+face.verticies[i].z));
								}
								gl.glEnd();
							}
						}
					}
					
				}
			}
		}
		gl.glDisable(GL2.GL_TEXTURE_2D);
	}
	private void display_2D(GL2 gl)
	{
		GLU glu = new GLU();
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		Dimension res = Parser.parseDimension(coyote.getSettings().getString(Settings.RESOLUTION));
		glu.gluOrtho2D(0,res.getWidth(),0,res.getHeight());
		//gl.glDisable(GL2.GL_LIGHT0);
		//gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		if(mouselook)
		{
			gl.glPolygonMode(GL.GL_FRONT,GL2.GL_FILL);				
			gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_BLACK,0);
			gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_BLACK,0);
			gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{0f,0f,0f,1.0f},0);
			gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
			
			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2f((float)((res.getWidth()/2.0)-8.0),(float)((res.getHeight()/2.0)-8.0));
			gl.glVertex2f((float)((res.getWidth()/2.0)-8.0),(float)((res.getHeight()/2.0)+8.0));
			gl.glVertex2f((float)((res.getWidth()/2.0)+8.0),(float)((res.getHeight()/2.0)+8.0));
			gl.glVertex2f((float)((res.getWidth()/2.0)+8.0),(float)((res.getHeight()/2.0)-8.0));
			gl.glEnd();
		}
		display_ui(gl,res.getWidth(),res.getHeight());	
		
		gl.glPopMatrix();
		////////////////////////////////////////////////////////
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0,1,0,1);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		
		
		//Disable Textures
		gl.glDisable(GL2.GL_TEXTURE_2D);
		//Set Foreground Color to Black
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,new float[]{0f,0f,0f,1.0f},0);
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{0f,0f,0f,1.0f},0);
		gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{0f},0);
		
		display_mode(gl);
		display_info(gl);
		
		gl.glPopMatrix();
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}
	private void display_mode(GL2 gl)
	{
		Engine engine = coyote.getEngine();
		//Settings settings = game.getSettings();
		GLUT glut = new GLUT();
		
		gl.glRasterPos2f(.05f,.95f);
		glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24,engine.currentMode.label);
	}
	private void display_info(GL2 gl)
	{
		Engine engine = coyote.getEngine();
		Settings settings = coyote.getSettings();
		GLUT glut = new GLUT();

		float x = .70f;
		float y = .95f;
		if(settings.getBoolean(Settings.DISPLAY_REAL_WORLD_TIME))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Real-World Time: "+new SimpleDateFormat("HH:mm:ss").format(new Date()));
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_IN_GAME_TIME))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"In-Game Time: "+new SimpleDateFormat("HH:mm:ss").format(new Date()));
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_PLAY_TIME))
		{
			gl.glRasterPos2f(x,y);
			long playtime = System.currentTimeMillis()-engine.startTime;		
			long seconds = TimeUnit.MILLISECONDS.toSeconds(playtime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(playtime));
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Play Time: "+TimeUnit.MILLISECONDS.toMinutes(playtime)+":"+(seconds<10?("0"+seconds):seconds));
			//glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24,"Play Time: "+String.format("%d:%dd",TimeUnit.MILLISECONDS.toMinutes(playtime),TimeUnit.MILLISECONDS.toSeconds(playtime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(playtime))));
			//glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_24,"Play Time: "+String.format("%d:%d",TimeUnit.MILLISECONDS.toMinutes(playtime),TimeUnit.MILLISECONDS.toSeconds(playtime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(playtime))));
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_MAP_NAME))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Current Map: "+engine.currentMap.name+" ("+engine.currentMapID+")");
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_WORLD_NAME))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Current World: "+engine.currentWorld.name+" ("+engine.currentWorldID+")");
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_REGION_NAME))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Current Region: "+engine.currentRegion.name+" ("+engine.currentRegionID+")");
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_LOCATION))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Current Camera Location = "+(Math.round(cameras[currentCamera].x*100)/100)+","+(Math.round(cameras[currentCamera].y*100)/100)+","+(Math.round(cameras[currentCamera].z*100)/100));
			y -= .05f;
		}
		if(settings.getBoolean(Settings.DISPLAY_ORIENTATION))
		{
			gl.glRasterPos2f(x,y);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18,"Current Camera Orientation = "+(Math.round(cameras[currentCamera].roll*1000)/1000)+","+(Math.round(cameras[currentCamera].pitch*1000)/1000)+","+(Math.round(cameras[currentCamera].heading*1000)/1000));
			y -= .05f;
		}
	}
	private void display_ui(GL2 gl,double width,double height)
	{
		Engine engine = coyote.getEngine();
		//LinkedHashMap<Integer,NGLIcon> icons = engine.icons;
		gl.glPolygonMode(GL.GL_FRONT,GL2.GL_FILL);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL2.GL_MODULATE);
		
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_AMBIENT,COLOR_WHITE,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_DIFFUSE,COLOR_WHITE,0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SPECULAR,new float[]{1f,1f,1f,1.0f},0);
		gl.glMaterialfv(GL2.GL_FRONT,GL2.GL_SHININESS,new float[]{0f},0);
		
		float x = 30f;
		float y = (float)(height - 90);
		
		for(int i = 0; i < hud.tools.length; i++)
		{
			NGLTool t = hud.tools[i];
			if(hud.currentTool==i)
			{
				t.icon.bindSelected(gl);
				t.icon.enableSelected(gl);
			}
			else
			{
				t.icon.bindNormal(gl);
				t.icon.enableNormal(gl);
			}
			
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0f,1f);
		    gl.glVertex2f(x,y);
		    gl.glTexCoord2f(0f,0f);
			gl.glVertex2f(x,y+30);
			gl.glTexCoord2f(1f,0f);
			gl.glVertex2f(x+30,y+30);
			gl.glTexCoord2f(1f,1f);
			gl.glVertex2f(x+30,y);
			gl.glEnd();
			
			if(hud.currentTool==i)
			{
				for(int j = 0; j < hud.tools.length; j++)
				{
					NGLIcon icon_obj = t.objects[j];
					icon_obj.bindNormal(gl);
					icon_obj.enableNormal(gl);
					gl.glBegin(GL2.GL_QUADS);
					gl.glTexCoord2f(0f,1f);
				    gl.glVertex2f(x+45,y);
				    gl.glTexCoord2f(0f,0f);
					gl.glVertex2f(x+45,y+30);
					gl.glTexCoord2f(1f,0f);
					gl.glVertex2f(x+75,y+30);
					gl.glTexCoord2f(1f,1f);
					gl.glVertex2f(x+75,y);
					gl.glEnd();
				}
			}
			y -= 60;
		}
	}
	
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
	{
		//display(drawable);
	}
	public void init(GLAutoDrawable drawable)
	{
		GL2 gl = (GL2)drawable.getGL();

		gl.glClearColor(1.0f,1.0f,1.0f,0.0f);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_FILL);
		gl.glLineWidth(3.0f);

		try{init_hud(gl);}catch(Exception e){e.printStackTrace();System.exit(-1);}
		try{init_texture(gl);}catch(Exception e){e.printStackTrace();System.exit(-1);}
		init_light(gl);
		
		/*
		
		GL gl = glDrawable.getGL();
		GLU glu = new GLU();

		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(-1.0f, 1.0f, -1.0f, 1.0f); // drawing square
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		//Model
		gl.glClearColor(1.0f,1.0f,1.0f,0.0f);
		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL.GL_FILL);
		gl.glLineWidth(3.0f);
		//Light
		float mat_specular[] = {1.0f,1.0f,1.0f,1.0f};
		float mat_shininess[] = {50.0f};
		float light_position[] = {0.0f,1.0f,.4f,0.0f};
		float white_light[] = {.5f,.5f,.5f,1.0f};
		float lmodel_ambient[] = {.5f,.5f,.5f,1.0f};
		float mat_amb_diff[] = {.5f,.5f,.5f,1.0f};
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_AMBIENT_AND_DIFFUSE,mat_amb_diff,0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_SPECULAR,mat_specular,0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_SHININESS,mat_shininess,0);
		gl.glLightfv(GL.GL_LIGHT0,GL.GL_POSITION,light_position,0);
		gl.glLightfv(GL.GL_LIGHT0,GL.GL_DIFFUSE,white_light,0);
		gl.glLightfv(GL.GL_LIGHT0,GL.GL_SPECULAR,white_light,0);
		gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT,lmodel_ambient,0);
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_DEPTH_TEST);
		//Textures
		gl.glShadeModel(GL.GL_FLAT);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV,GL.GL_TEXTURE_ENV_MODE,GL.GL_REPLACE);
		//Bezier Patches
		gl.glEnable(GL.GL_MAP2_VERTEX_3);
		gl.glEnable(GL.GL_MAP2_TEXTURE_COORD_2);
		gl.glEnable(GL.GL_AUTO_NORMAL);
		gl.glMapGrid2f(20,0.0f,1.0f,20,0.0f,1.0f);
		*/
	}
	
	public void init_hud(GL2 gl)
	{		
		Engine engine = coyote.getEngine();
		for(int i: engine.icons.keySet())
			engine.icons.get(i).compile();	
	}
	public void init_texture(GL2 gl) throws GLException, IOException, ScriptException
	{
		Engine engine = coyote.getEngine();
		//gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_S,GL.GL_REPEAT);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_T,GL.GL_REPEAT);
		for(int i: engine.textures.keySet())
			engine.textures.get(i).compile();
	}
	public void init_light(GL2 gl)
	{
		//gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_AMBIENT_AND_DIFFUSE,new float[]{.5f,.5f,.5f,1.0f},0);
		//gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SPECULAR,new float[]{0f,0f,0f,1.0f},0);
		//gl.glMaterialfv(GL2.GL_FRONT_AND_BACK,GL2.GL_SHININESS,new float[]{0f},0);
		
		//gl.glLightModelfv(GL2.GL_AMBIENT,new float[]{1f,0f,1f,1.0f},0);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_AMBIENT,new float[]{.25f,.25f,0.0f,1.0f},0);
		gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_DIFFUSE,new float[]{1.0f,1.0f,0.0f,1.0f},0);
		gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_SPECULAR,new float[]{1.0f,1.0f,1.0f,0.0f},0);
		//gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_POSITION,new float[]{593f,40f,1190f,1f},0);//ZERO as Last # in GL_POSITION indicates directional light
		gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_POSITION,new float[]{-1f,-.25f,0.1f,0f},0);
		
		gl.glEnable(GL2.GL_LIGHT0);	
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}
		
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,int height)
	{
		NGLCamera c = cameras[currentCamera];
		GL2 gl = drawable.getGL().getGL2();
		GLU glu = new GLU();
		gl.glViewport(0,0,width,height);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(c.angle,c.aspect,c.near,c.far);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}
	public void dispose(GLAutoDrawable arg0)
	{	
	}
	 
	
	public void compile_display_list(final int list,GL2 gl)
	{
		/*gl.glNewList(list,GL.GL_COMPILE);
		gl.glPolygonMode(GL.GL_FRONT,GL.GL_FILL);
        gl.glBegin(GL.GL_QUADS);
		for(int y = 0; y < engine.map.length; y++)
		{
			for(int x = 0; x < engine.map[0].length; x++)
			{
				gl.glColor4f(engine.map[y][x].terrain.r,engine.map[y][x].terrain.g,engine.map[y][x].terrain.b,engine.map[y][x].terrain.a);
				gl.glVertex3f((float)(x*Cell.SIZE),(float)engine.map[y][x].terrain.h,(float)(y*Cell.SIZE));
				gl.glVertex3f((float)(x*Cell.SIZE),(float)engine.map[y][x].terrain.h,(float)((y+1)*Cell.SIZE));
				gl.glVertex3f((float)((x+1)*Cell.SIZE),(float)engine.map[y][x].terrain.h,(float)((y+1)*Cell.SIZE));
				gl.glVertex3f((float)((x+1)*Cell.SIZE),(float)engine.map[y][x].terrain.h,(float)(y*Cell.SIZE));
			}
		}
		gl.glEnd();
		gl.glEndList();*/
	}
		
	public void mouseWheelMoved(MouseWheelEvent e)
	{		
		if(cameras[currentCamera].type==NGLCamera.TYPE_RTS)
		{
			NGLTerrain t = coyote.getEngine().terrain;
			//Adjust for terrain
			int cell_x = Math.min(Math.max((int)(cameras[currentCamera].x/10.0),0),coyote.getEngine().terrain.num_cols-3);
			int cell_z =  Math.min(Math.max((int)(cameras[currentCamera].z/10.0),0),coyote.getEngine().terrain.num_rows-3);
			double h = 1.9+((t.verticies[cell_z][cell_x]+t.verticies[cell_z][cell_x+1]+t.verticies[cell_z+1][cell_x]+t.verticies[cell_z+1][cell_x+1])/4);
			cameras[currentCamera].y = Math.min(2000,Math.max(h,cameras[currentCamera].y+4*e.getWheelRotation()));
		}
		else if(cameras[currentCamera].type==NGLCamera.TYPE_FLYBY)
		{
			
		}
		else if(cameras[currentCamera].type==NGLCamera.TYPE_FPS)
		{
			
		}
	}	
	public void keyPressed(KeyEvent ke)
	{
		Engine engine = coyote.getEngine();
		KeyBindings bindings = coyote.getSettings().getKeyBindings();
		//System.err.println("KeyPressed Event "+ke.getKeyChar());
		if(ke.getKeyCode()==KeyEvent.VK_1)
		{
			engine.currentMode = engine.modes[0];
			currentCamera = engine.currentMode.camera;
			mouselook = engine.currentMode.defaultMouselook;
			canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
		}
		else if(ke.getKeyCode()==KeyEvent.VK_2)
		{
			engine.currentMode = engine.modes[1];
			currentCamera = engine.currentMode.camera;
			mouselook = engine.currentMode.defaultMouselook;
			canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
		}
		else if(ke.getKeyCode()==KeyEvent.VK_3)
		{
			engine.currentMode = engine.modes[2];
			currentCamera = engine.currentMode.camera;
			mouselook = engine.currentMode.defaultMouselook;
			canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
		}
		
		if(currentCamera==NGLCamera.TYPE_FPS)
		{
			if(ke.getKeyCode()==bindings.forward_1) nav_forward = true;
			else if(ke.getKeyCode()==bindings.backward_1) nav_backward = true;
			else if(ke.getKeyCode()==bindings.turn_left_1) nav_turn_left = true;
			else if(ke.getKeyCode()==bindings.turn_right_1) nav_turn_right = true;
			else if(ke.getKeyCode()==bindings.strafe_left_1) nav_strafe_left = true;
			else if(ke.getKeyCode()==bindings.strafe_right_1) nav_strafe_right = true;
			else if(ke.getKeyCode()==bindings.toggle_mouselook&&engine.currentMode.toggleMouselook)
			{
				if(!mouselook_locked)
				{
					mouselook_locked = true;
					mouselook = !mouselook;
					canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
				}
			}
			else if(ke.getKeyCode()==bindings.ui_menu){engine.execute_command_builtin("DEACTIVATE_CANVAS","0");engine.execute_command_builtin("LAUNCH_DIALOG","4");}
		}
		else if(currentCamera==NGLCamera.TYPE_RTS)
		{
			if(ke.getKeyCode()==bindings.pan_up_1) nav_pan_up = true;
			else if(ke.getKeyCode()==bindings.pan_down_1) nav_pan_down = true;
			else if(ke.getKeyCode()==bindings.pan_left_1) nav_pan_left = true;
			else if(ke.getKeyCode()==bindings.pan_right_1) nav_pan_right = true;
			else if(ke.getKeyCode()==bindings.toggle_mouselook&&engine.currentMode.toggleMouselook)
			{
				if(!mouselook_locked)
				{
					mouselook_locked = true;
					mouselook = !mouselook;
					canvas.setCursor(mouselook?engine.cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT):engine.currentMode.cursor);
				}
			}
			else if(ke.getKeyCode()==bindings.ui_menu){engine.execute_command_builtin("DEACTIVATE_CANVAS","0");engine.execute_command_builtin("LAUNCH_DIALOG","4");}
		}
		
		
		/*
		char key = ke.getKeyChar();
		if(key=='w') nav_forward = true;
		else if(key=='a') nav_strafe_left = true;
		else if(key=='s') nav_backward = true;
		else if(key=='d') nav_strafe_right = true;
		else if(ke.getKeyCode()==KeyEvent.VK_UP) up = true;
		else if(ke.getKeyCode()==KeyEvent.VK_DOWN) down = true;
		else if(ke.getKeyCode()==KeyEvent.VK_LEFT) left = true;
		else if(ke.getKeyCode()==KeyEvent.VK_RIGHT) right = true;
		else if(ke.getKeyCode()==KeyEvent.VK_ESCAPE)
1ALOG","4");
		}*/
	}
	public void keyReleased(KeyEvent ke)
	{
		/*char key = ke.getKeyChar();
		if(key=='w') nav_forward = false;
		else if(key=='a') nav_strafe_left = false;
		else if(key=='s') nav_backward = false;
		else if(key=='d') nav_strafe_right = false;
		else if(ke.getKeyCode()==KeyEvent.VK_UP) up = false;
		else if(ke.getKeyCode()==KeyEvent.VK_DOWN) down = false;
		else if(ke.getKeyCode()==KeyEvent.VK_LEFT) left = false;
		else if(ke.getKeyCode()==KeyEvent.VK_RIGHT)w right = false;*/
		KeyBindings bindings = coyote.getSettings().getKeyBindings();
		
		if(currentCamera==NGLCamera.TYPE_FPS)
		{
			if(ke.getKeyCode()==bindings.forward_1) nav_forward = false;
			else if(ke.getKeyCode()==bindings.backward_1) nav_backward = false;
			else if(ke.getKeyCode()==bindings.turn_left_1) nav_turn_left = false;
			else if(ke.getKeyCode()==bindings.turn_right_1) nav_turn_right = false;
			else if(ke.getKeyCode()==bindings.strafe_left_1) nav_strafe_left = false;
			else if(ke.getKeyCode()==bindings.strafe_right_1) nav_strafe_right = false;
			else if(ke.getKeyCode()==bindings.toggle_mouselook&&coyote.getEngine().currentMode.toggleMouselook)
			{
				mouselook_locked = false;
			}
		}
		else if(currentCamera==NGLCamera.TYPE_RTS)
		{
			if(ke.getKeyCode()==bindings.pan_up_1) nav_pan_up = false;
			else if(ke.getKeyCode()==bindings.pan_down_1) nav_pan_down = false;
			else if(ke.getKeyCode()==bindings.pan_left_1) nav_pan_left = false;
			else if(ke.getKeyCode()==bindings.pan_right_1) nav_pan_right = false;
			else if(ke.getKeyCode()==bindings.toggle_mouselook&&coyote.getEngine().currentMode.toggleMouselook)
			{
				mouselook_locked = false;
			}
		}
		
	}
	public void keyTyped(KeyEvent ke)
	{
		NGLCamera c = cameras[currentCamera];
		if(c.type==NGLCamera.TYPE_RTS)
		{
			
		}
		else if(c.type==NGLCamera.TYPE_FLYBY)
		{
			if(ke.getKeyChar()==' ')
			{
				System.err.println("Spacebar Typed");
				cameras[currentCamera].stable = !cameras[currentCamera].stable;
			}
		}
		else if(c.type==NGLCamera.TYPE_FPS)
		{
			
		}
		/*if(ke.getKeyChar()=='c')
		{
			System.err.println("Current Camera Location = "+cameras[currentCamera].x+","+cameras[currentCamera].y+","+cameras[currentCamera].z);
			System.err.println("Current Camera Orientation = "+cameras[currentCamera].roll+","+cameras[currentCamera].pitch+","+cameras[currentCamera].heading);
		}*/
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		// TODO Auto-generated method stub	
	}
	@Override
	public void mouseEntered(MouseEvent e)
	{
	}
	@Override
	public void mouseExited(MouseEvent e)
	{
		NGLCamera c = cameras[currentCamera];
		if(c.type==NGLCamera.TYPE_RTS)
		{
			
		}
		else if(c.type==NGLCamera.TYPE_FLYBY)
		{
			mouse_x = 0;
			mouse_y = 0;
		}
		else if(c.type==NGLCamera.TYPE_FPS)
		{
		}
		
	}
	@Override
	public void mousePressed(MouseEvent e)
	{
		System.err.println("Mouse Pressed");
		if(e.getButton()==MouseEvent.BUTTON1)
		{
			mouse_1 = true;
			
			if(!mouselook)
			{
				mouse_x = e.getX();
				mouse_y = height-e.getY()-1;

				hud.pressed(mouse_x,mouse_y);
			}
			else
			{
				selection.start();
			}
			/*
			if()
			{
				
			}
			else
			{
				selection.start();
			}*/
		}
		else if(e.getButton()==MouseEvent.BUTTON2)// Mouse Wheel
		{
		}
		else if(e.getButton()==MouseEvent.BUTTON3)//Right Click
		{
			hud.currentTool = -1;
			if(coyote.getEngine().currentMode.toggleMouselook)
			{
				mouselook = true;
				canvas.setCursor(coyote.getEngine().cursors.getCursor(NovaCursorManager.TYPE_TRANSPARENT));
			}
		}	
	}
	@Override
	public void mouseReleased(MouseEvent e)
	{
		System.err.println("Mouse Released");
		if(e.getButton()==MouseEvent.BUTTON1)
		{
			mouse_1 = false;
			
			if(!mouselook)
			{
				mouse_x = e.getX();
				mouse_y = height-e.getY()-1;
				hud.released(mouse_x,mouse_y);
			}
			else
			{
				selection.end();
			}
			
			
			if(selection.mode==NGLTerrainSelection.MODE_DRAG_LINE)
			{
				//Do Something
				selection.clearBuffer();
			}
			else if(selection.mode==NGLTerrainSelection.MODE_DRAG_RECTANGLE)
			{
				//Do Something
				selection.clearBuffer();
			} 
			else if(selection.mode==NGLTerrainSelection.MODE_FREEHAND)
			{
				selection.calculate();
				//Do something with the selection
				selection.clearBuffer();
			}
		}
		else if(e.getButton()==MouseEvent.BUTTON3)//Right Click
		{
			if(coyote.getEngine().currentMode.toggleMouselook)
			{
				mouselook = false;
				canvas.setCursor(coyote.getEngine().currentMode.cursor);
			}
		}
	}
	@Override
	public void mouseDragged(MouseEvent e)
	{
		// TODO Auto-generated method stub
	}
	@Override
	public void mouseMoved(MouseEvent e)
	{
		System.err.println("Mouse Moved Event");
		if(mouselook)
		{
			mouse_x = e.getX()-(width/2);
			mouse_y = -(e.getY()-(height/2));
			System.err.println("mouse_x: "+mouse_x);
			System.err.println("mouse_y: "+mouse_y);
		}
		else
		{
			mouse_x = e.getX();
			mouse_y = height-e.getY()-1;
			System.err.println("mouse_x: "+mouse_x);
			System.err.println("mouse_y: "+mouse_y);
		}
		
	}

	//Focus Listener
	public void focusGained(FocusEvent arg0)
	{
		System.err.println("Focus Gained");
		this.canvas.display();
		//this.reshape(640,0,1280,750);
	}
	public void focusLost(FocusEvent arg0)
	{
	}
	
	//ComponentListener
	public void componentHidden(ComponentEvent e)
	{
		
	}

	public void componentMoved(ComponentEvent e)
	{

	}

	public void componentResized(ComponentEvent e)
	{
		this.width = this.getWidth();
		this.height = this.getHeight();
	}
	public void componentShown(ComponentEvent e)
	{
		
	}
}

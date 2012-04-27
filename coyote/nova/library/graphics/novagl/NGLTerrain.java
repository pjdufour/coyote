package nova.library.graphics.novagl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import nova.library.utilities.Parser;
import nova.library.graphics.textures.BalboaTexture;
import nova.library.graphics.vecmath.Face3d;
import nova.library.graphics.vecmath.Vector3d;
import nova.library.graphics.vecmath.Vertex3d;
import nova.library.logs.Logs;

public class NGLTerrain
{
	public int num_rows;
	public int num_cols;
	public int cell_size;
	private double no_data_value;
	private double lower_left_corner_x;
	private double lower_left_corner_y;
	public boolean compiled;
	
	
	/*
	 * Arrays will be indexed to the lower left corner as the origin
	 * Therefore the arrays will be processed as x+,then y+ or
	 * an increase in the y index moves "up"/north
	 * and an increase in the x index moves "right"/east
	 * 
	 * Also, heightmaps will be 1 unit wider and longer than actual cells in the game 
	 * 
	 * So....
	 * heightmaps are the heights of the corners of the cells not the actual cells.
	 * so....
	 * how am I going to overlay a cell raster on the heightmap raster
	 */
	public double heightmap[][];
	public double verticies[][];
	public Vector3d normals[][];// For Smooth Terrain
	public Vector3d planes[][];//For Selection and collision detection
	public Vector3d normals_triangles[][][];//[Y][X][0|1] (For Ray Detection)
	public int landcover[][];
	
	private Logs logs;
	private Engine engine;

	public NGLTerrain(Engine engine,Logs logs,String name) throws IOException, SQLException
	{
		this.engine = engine;
		this.logs = logs;
		this.compiled = false;
		load(name);
	}
	public NGLTerrain(Engine engine,Logs logs,String name,String hdr,String flt) throws IOException, SQLException
	{
		this.engine = engine;
		this.logs = logs;
		this.compiled = false;
		load(hdr,flt);
	}
	public void load(String name) throws IOException, SQLException
	{
		try{Class.forName("com.mysql.jdbc.Driver").newInstance();}catch(Exception e){System.err.println("Could not load MySQL Driver");e.printStackTrace();System.exit(-1);}
		Connection connection = DriverManager.getConnection("jdbc:mysql://10.34.0.1/", "svn", "2LM38c4huNSncrB4");
		ResultSet rs = connection.createStatement().executeQuery("select header,heightmap from test.map where name='"+name+"';");
		rs.next();
		load_header(rs.getString(1));
		load_heightmap(rs.getString(2));
		load_verticies();
		load_normals();
		load_planes();
		load_landcover();
	}
	public void load(String hdr,String flt) throws IOException, SQLException
	{
		load_header(hdr);
		load_heightmap(flt);
		load_verticies();
		load_normals();
		load_planes();
		load_landcover();
		frag_terrain(1);
	}
	
	public void load_header(String filename)
	{
		if(engine.log==3) logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Loading Terrain Header from "+filename);
		else if(engine.log==2) logs.addToLog(Logs.LOG_SYSTEM_DEBUG,"Loading Terrain Header from "+filename);
		
		for(String line: Parser.fileToList(filename,GLOBALS.PREFIX_COMMENT))
		{
			String str[] = line.split("\\s+");
			if(str[0].equalsIgnoreCase("ncols"))
				num_cols = Integer.parseInt(str[1]);
			else if(str[0].equalsIgnoreCase("nrows"))
				num_rows = Integer.parseInt(str[1]);
			else if(str[0].equalsIgnoreCase("xllcorner"))
				lower_left_corner_x = Double.parseDouble(str[1]);
			else if(str[0].equalsIgnoreCase("yllcorner"))
				lower_left_corner_y = Double.parseDouble(str[1]);
			else if(str[0].equalsIgnoreCase("cellsize"))
				cell_size = Integer.parseInt(str[1]);
			else if(str[0].equalsIgnoreCase("NODATA_value"))
				no_data_value = Double.parseDouble(str[1]);
		}
		if(engine.log==3)
		{
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"ncols = "+num_cols);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"num_rows = "+num_rows);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"cell_size = "+cell_size);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"NoData = "+no_data_value);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Done Loading Terrain Header from "+filename);
		}
		else if(engine.log==2)
		{
			logs.addToLog(Logs.LOG_SYSTEM_DEBUG,"Done Loading Terrain Header from "+filename);
		}
		//System.exit(-1);
	}
	public void load_heightmap(String filename) throws IOException
	{
		if(engine.log==3) logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Loading Terrain Heightmap from "+filename);
		else if(engine.log==2) logs.addToLog(Logs.LOG_SYSTEM_DEBUG,"Loading Terrain Heightmap from "+filename);
		InputStream file;
		try{file = Parser.class.getResourceAsStream(filename);if(file==null)throw new Exception();}
		catch(Exception e)
		{
			//e.printStackTrace();
			file = new FileInputStream(new File(filename));
		};
		
		heightmap = new double[num_rows][num_cols];
		double max = 0.0,min = 100.0;
		for(int z = 0; z < heightmap.length; z++)
		{
			for(int x = 0; x < heightmap[0].length; x++)
			{
				byte buffer[] = new byte[4];
				file.read(buffer);
				heightmap[num_rows-z-1][x]=(double)(Float.intBitsToFloat(((0xff & buffer[0])|((0xff & buffer[1])<<8)|((0xff & buffer[2])<<16)|((0xff&buffer[3])<<24))));
				
				if(heightmap[num_rows-z-1][x]==no_data_value) {System.err.println("No data value");System.exit(-1);}
				if(heightmap[num_rows-z-1][x]>max) max = heightmap[num_rows-z-1][x];
				if(heightmap[num_rows-z-1][x]<min) min = heightmap[num_rows-z-1][x];
			}
			//System.exit(-1);
		}
		if(engine.log==3)
		{
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Maximum = "+max);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Minimum = "+min);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Done Loading Terrain Heightmap from "+filename);
		}
		else if(engine.log==2)
		{
			logs.addToLog(Logs.LOG_SYSTEM_DEBUG,"Done Loading Terrain Header from "+filename);
			logs.addToLogs((new int[]{Logs.LOG_SYSTEM_INFO,Logs.LOG_SYSTEM_DEBUG}),"Done Loading Terrain Heightmap from "+filename);
		}
	}
	public void load_verticies()
	{
		verticies = new double[num_rows-1][num_cols-1];
		for(int z = 0; z < verticies.length; z++)
		{
			for(int x = 0; x < verticies[0].length; x++)
			{
				verticies[z][x] = (heightmap[z][x]+heightmap[z][x+1]+heightmap[z+1][x]+heightmap[z+1][x+1])/4;
			}
		}
	}
	public void load_normals()
	{
		normals = new Vector3d[num_rows-1][num_cols-1];
		for(int z = 0; z < normals.length; z++)
		{
			for(int x = 0; x < normals[0].length; x++)
			{
				//Moving From 12 O'clock counterclockwise
				Vector3d v0 = new Vector3d(0.0,heightmap[z+1][x]-heightmap[z][x],-1.0*cell_size);
				Vector3d v1 = new Vector3d(-1.0*cell_size,heightmap[z][x+1]-heightmap[z][x],0.0);
				Vector3d v2 = new Vector3d(0.0,heightmap[z+1][x+1]-heightmap[z][x+1],1.0*cell_size);
				Vector3d v3 = new Vector3d(1.0*cell_size,heightmap[z+1][x+1]-heightmap[z+1][x],0.0);
				Vector3d n = new Vector3d(0.0,0.0,0.0);
				n.add(Vector3d.normalize(Vector3d.cross(v0,v1)));
				n.add(Vector3d.normalize(Vector3d.cross(v1,v2)));
				n.add(Vector3d.normalize(Vector3d.cross(v2,v3)));
				n.add(Vector3d.normalize(Vector3d.cross(v3,v0)));
				if(Math.sqrt((n.x*n.x)+(n.y*n.y)+(n.z*n.z))==0){System.err.println("Magnitude = 0 on Terrain Cell ["+z+"]["+x+"]");System.exit(-1);}
				n = Vector3d.normalize(n);
				normals[z][x] = n;
			}
		}
		normals_triangles = new Vector3d[num_rows-2][num_cols-2][2];
		for(int z = 0; z < normals_triangles.length; z++)
		{
			for(int x = 0; x < normals_triangles[0].length; x++)
			{
				//Moving From 12 O'clock counterclockwise
				Vector3d v0 = new Vector3d(0.0,verticies[z+1][x]-verticies[z][x],-1.0*cell_size);
				Vector3d v1 = new Vector3d(-1.0*cell_size,verticies[z][x+1]-verticies[z][x],0.0);
				Vector3d v2 = new Vector3d(0.0,verticies[z+1][x+1]-verticies[z][x+1],1.0*cell_size);
				Vector3d v3 = new Vector3d(1.0*cell_size,verticies[z+1][x+1]-verticies[z+1][x],0.0);
				Vector3d n1 = new Vector3d(0.0,0.0,0.0);
				n1 = Vector3d.normalize(Vector3d.cross(v0,v1));
				Vector3d n2 = new Vector3d(0.0,0.0,0.0);
				n2 = Vector3d.normalize(Vector3d.cross(v2,v3));
				if(Math.sqrt((n1.x*n1.x)+(n1.y*n1.y)+(n1.z*n1.z))==0){System.err.println("Magnitude = 0 on Terrain Cell ["+z+"]["+x+"]");System.exit(-1);}
				if(Math.sqrt((n2.x*n2.x)+(n2.y*n2.y)+(n2.z*n2.z))==0){System.err.println("Magnitude = 0 on Terrain Cell ["+z+"]["+x+"]");System.exit(-1);}
				normals_triangles[z][x][0] = n1;
				normals_triangles[z][x][1] = n2;
			}
		}
		//System.exit(-1);
		
		/*
		 * Vector3d v0 = null;
		Vector3d v1 = null;
		Vector3d v2 = null;
		Vector3d v3 = null;
		Vector3d n = new Vector3d(0.0,0.0,0.0);
		
		//Moving From 12 O'clock counterclockwise
		if(z>0) v0 = new Vector3d(0.0,heightmap[z-1][x]-heightmap[z][x],-1.0*cell_size);
		if(x>0) v1 = new Vector3d(-1.0*cell_size,heightmap[z][x-1]-heightmap[z][x],0.0);
		if(z<heightmap.length-1) v2 = new Vector3d(0.0,heightmap[z+1][x]-heightmap[z][x],1.0*cell_size);
		if(x<heightmap[0].length-1) v3 =new Vector3d(1.0*cell_size,heightmap[z][x+1]-heightmap[z][x],0.0);
		
		if(v0!=null&&v1!=null) n.add(Vector3d.normalize(Vector3d.cross(v0,v1)));
		if(v1!=null&&v2!=null) n.add(Vector3d.normalize(Vector3d.cross(v1,v2)));
		if(v2!=null&&v3!=null) n.add(Vector3d.normalize(Vector3d.cross(v2,v3)));
		if(v3!=null&&v0!=null) n.add(Vector3d.normalize(Vector3d.cross(v3,v0)));
		if(Math.sqrt((n.x*n.x)+(n.y*n.y)+(n.z*n.z))==0){System.err.println("Magnitude = 0 on Terrain Cell ["+z+"]["+x+"]");System.exit(-1);}
		normals[z][x] = n;
		 */
	}
	public void load_planes()
	{
		planes = new Vector3d[num_rows-2][num_cols-2];
		for(int z = 0; z < planes.length; z++)
		{
			for(int x = 0; x < planes[0].length; x++)
			{
				double nx = (normals[z][x].x+normals[z+1][x].x+normals[z][x+1].x+normals[z+1][x+1].x)/4.0;
				double ny = (normals[z][x].y+normals[z+1][x].y+normals[z][x+1].y+normals[z+1][x+1].y)/4.0;
				double nz = (normals[z][x].z+normals[z+1][x].z+normals[z][x+1].z+normals[z+1][x+1].z)/4.0;
				planes[z][x] = new Vector3d(nx,ny,nz);
			}
		}
	}
	public void load_landcover()
	{
		landcover = new int[num_rows-2][num_cols-2];
		for(int z = 0; z < landcover.length; z++)
		{
			for(int x = 0; x < landcover[0].length; x++)
			{
				//textures[z][x] = NGLTexture.TEXTURE_GRASS;
				landcover[z][x] = 0;
			}
		}
	}
	/*
	 * if split == 2,
	 */
	public void frag_terrain(int f)
	{
		if(f>1)
		{
			double new_verticies[][] = new double[(verticies.length-1)*f+1][(verticies[0].length-1)*f+1];
			Vector3d new_normals[][] = new Vector3d[(normals.length-1)*f+1][(normals[0].length-1)*f+1];
			int new_landcover[][] = new int[landcover.length*f][landcover[0].length*f];
			
			
			for(int z = 0; z < verticies.length; z++)
			{
				for(int x = 0; x < verticies[0].length; x++)
				{
					for(int a = 0; ((z*f+a) < new_verticies.length) && a < f; a++)
					{
						for(int b = 0; ((x*f+b) < new_verticies[0].length) && b < f; b++)
						{
							/*if(a==0&&b==0)
							{
								new_verticies[z*f][x*f] = verticies[z][x];
								new_normals[z*f][x*f] = normals[z][x];
								if((z < textures.length)&&(x < textures[0].length))
								{
									new_textures[z*f+a][x*f+b] = textures[z][x];
								}
							}
							
							//Go down the line only averaging the next point if it exists*/
							try
							{
								double wz = (((f*1.0)-(a*1.0))/(f*1.0));
								double wx = (((f*1.0)-(b*1.0))/(f*1.0));
								//double total_weight = wz*wx+wz*(1.0-wx)+(1.0-wz)*wx+(1.0-wz)*(1.0-wx);
								//This Also happens to prevent weighting against verticies that do not exist
								if(wz==1.0&&wx==1.0)
								{
									new_verticies[z*f+a][x*f+b] = verticies[z][x];
									new_normals[z*f][x*f] = normals[z][x];
									if((z < landcover.length)&&(x < landcover[0].length))
										new_landcover[z*f+a][x*f+b] = landcover[z][x];
								}
								else if(wz!=1.0&&wx==1.0)
								{
									new_verticies[z*f+a][x*f+b] = verticies[z][x]*wz*wx+verticies[z+1][x]*(1.0-wz)*wx;
									double nx = normals[z][x].x*wz*wx+normals[z+1][x].x*(1.0-wz)*wx;
									double ny = normals[z][x].y*wz*wx+normals[z+1][x].y*(1.0-wz)*wx;
									double nz = normals[z][x].z*wz*wx+normals[z+1][x].z*(1.0-wz)*wx;
									new_normals[z*f+a][x*f+b] = new Vector3d(nx,ny,nz);
								}
								else if(wz==1.0&&wx!=1.0)
								{
									new_verticies[z*f+a][x*f+b] = verticies[z][x]*wz*wx+verticies[z][x+1]*wz*(1.0-wx);
									double nx = normals[z][x].x*wz*wx+normals[z][x+1].x*wz*(1.0-wx);
									double ny = normals[z][x].y*wz*wx+normals[z][x+1].y*wz*(1.0-wx);
									double nz = normals[z][x].z*wz*wx+normals[z][x+1].z*wz*(1.0-wx);
									new_normals[z*f+a][x*f+b] = new Vector3d(nx,ny,nz);
								}
								else
								{
									new_verticies[z*f+a][x*f+b] = verticies[z][x]*wz*wx+verticies[z+1][x]*(1.0-wz)*wx+verticies[z][x+1]*wz*(1.0-wx)+verticies[z+1][x+1]*(1.0-wz)*(1.0-wx);
									double nx = normals[z][x].x*wz*wx+normals[z+1][x].x*(1.0-wz)*wx+normals[z][x+1].x*wz*(1.0-wx)+normals[z+1][x+1].x*(1.0-wz)*(1.0-wx);
									double ny = normals[z][x].y*wz*wx+normals[z+1][x].y*(1.0-wz)*wx+normals[z][x+1].y*wz*(1.0-wx)+normals[z+1][x+1].y*(1.0-wz)*(1.0-wx);
									double nz = normals[z][x].z*wz*wx+normals[z+1][x].z*(1.0-wz)*wx+normals[z][x+1].z*wz*(1.0-wx)+normals[z+1][x+1].z*(1.0-wz)*(1.0-wx);
									new_normals[z*f+a][x*f+b] = new Vector3d(nx,ny,nz);
								}
									
									
								/*
								 double nx = (normals[z][x].x+normals[z+1][x].x+normals[z][x+1].x+normals[z+1][x+1].x)/4;
								double ny = (normals[z][x].y+normals[z+1][x].y+normals[z][x+1].y+normals[z+1][x+1].y)/4;
								double nz = (normals[z][x].z+normals[z+1][x].z+normals[z][x+1].z+normals[z+1][x+1].z)/4;
								new_normals[z*f+a][x*f+b] = new Vector3d(nx,ny,nz);
								 */
								if((z < verticies.length-1)&&(x < verticies[0].length-1))
								{
									new_landcover[z*f+a][x*f+b] = landcover[z][x];
								}
							}
							catch(Exception e)
							{
								System.err.println("-----------------------------------------------------");
								System.err.println("Num Verticies: "+verticies.length+","+verticies[0].length);
								System.err.println("Number of Normals: "+normals.length+","+normals[0].length);
								System.err.println("Number of Landcover: "+landcover.length+","+landcover[0].length);
								System.err.println("Number of Rows and Cols: "+num_rows+","+num_cols);
								System.err.println("New Num Verticies: "+new_verticies.length+","+new_verticies[0].length);
								System.err.println("New Number of Normals: "+new_normals.length+","+new_normals[0].length);
								System.err.println("New Number of Textures: "+new_landcover.length+","+new_landcover[0].length);
								System.err.println("-----------------------------------------------------");
								System.err.println("Old Z,X: "+z+","+x);
								System.err.println("A,B: "+a+","+b);
								System.err.println("New Z,X: "+(z*f+a)+","+(x*f+b));
								System.err.println("wz: "+(((f*1.0)-(a*1.0))/(f*1.0)));
								System.err.println("wx: "+(((f*1.0)-(b*1.0))/(f*1.0)));
								System.err.println("-----------------------------------------------------");
								e.printStackTrace();
								System.exit(-1);
							};
						}
					}
				}
			}
			verticies = new_verticies;
			normals = new_normals;
			landcover = new_landcover;
			num_rows = (num_rows-2)*f+2;
			num_cols = (num_cols-2)*f+2;
			cell_size /= f;
		}
		System.err.println("Num Verticies: "+verticies.length+","+verticies[0].length);
		System.err.println("Number of Normals: "+normals.length+","+normals[0].length);
		System.err.println("Number of Textures: "+landcover.length+","+landcover[0].length);
		System.err.println("Number of Rows and Cols: "+num_rows+","+num_cols);
	}

	public void compile(int list,GL2 gl)
	{
		compiled = true;
		int current_texture = 0;
		
		gl.glNewList(list,GL2.GL_COMPILE);
			//Texture
			gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_FILL);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL.GL_REPLACE);
			engine.textures.get(current_texture).texture.bind(gl);
			for(int z = 0; z < landcover.length; z++)
			{
				gl.glBegin(GL2.GL_QUADS);
				for(int x = 0; x < landcover[0].length; x++)
				{
					if(current_texture!=landcover[z][x])
					{
						current_texture = landcover[z][x];
						engine.textures.get(current_texture).texture.bind(gl);
					}
					
					BalboaTexture texture = engine.textures.get(current_texture);
					
				    gl.glTexCoord2f(texture.left,texture.bottom);
					gl.glNormal3f((float)normals[z][x].x,(float)normals[z][x].y,(float)normals[z][x].z);
					gl.glVertex3f((float)(x*cell_size),(float)verticies[z][x],(float)(z*cell_size));
					
					gl.glTexCoord2f(texture.left,texture.top);
					gl.glNormal3f((float)normals[z+1][x].x,(float)normals[z+1][x].y,(float)normals[z+1][x].z);
					gl.glVertex3f((float)(x*cell_size),(float)verticies[z+1][x],(float)((z+1)*cell_size));
					
					gl.glTexCoord2f(texture.right,texture.top);
					gl.glNormal3f((float)normals[z+1][x+1].x,(float)normals[z+1][x+1].y,(float)normals[z+1][x+1].z);
					gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z+1][x+1],(float)((z+1)*cell_size));
					
					gl.glTexCoord2f(texture.right,texture.bottom);
					gl.glNormal3f((float)normals[z][x+1].x,(float)normals[z][x+1].y,(float)normals[z][x+1].z);
					gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z][x+1],(float)(z*cell_size));
				}
				gl.glEnd();
			}
			gl.glDisable(GL2.GL_TEXTURE_2D);
			//Grid
			if(engine.display_grid)
			{
				gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_LINE);
				gl.glLineWidth(1f);
				for(int z = 0; z < landcover.length; z++)
				{
					gl.glBegin(GL2.GL_QUADS);
					for(int x = 0; x < landcover[0].length; x++)
					{
					    gl.glColor3f((float)0f,(float)0f,(float)0f);
						gl.glNormal3f((float)normals[z][x].x,(float)normals[z][x].y,(float)normals[z][x].z);
						gl.glVertex3f((float)(x*cell_size),(float)verticies[z][x],(float)(z*cell_size));
						gl.glNormal3f((float)normals[z+1][x].x,(float)normals[z+1][x].y,(float)normals[z+1][x].z);
						gl.glVertex3f((float)(x*cell_size),(float)verticies[z+1][x],(float)((z+1)*cell_size));
						gl.glNormal3f((float)normals[z+1][x+1].x,(float)normals[z+1][x+1].y,(float)normals[z+1][x+1].z);
						gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z+1][x+1],(float)((z+1)*cell_size));
						gl.glNormal3f((float)normals[z][x+1].x,(float)normals[z][x+1].y,(float)normals[z][x+1].z);
						gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z][x+1],(float)(z*cell_size));
					}
					gl.glEnd();
				}
			}
		gl.glEndList();
	}
	public void display(GL2 gl)
	{
		int current_texture = 0;
		gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_FILL);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV,GL2.GL_TEXTURE_ENV_MODE,GL.GL_REPLACE);
		engine.textures.get(current_texture).texture.bind(gl);
		for(int z = 0; z < landcover.length; z++)
		{
			gl.glBegin(GL2.GL_QUADS);
			for(int x = 0; x < landcover[0].length; x++)
			{
				if(current_texture!=landcover[z][x])
				{
					current_texture = landcover[z][x];
					engine.textures.get(current_texture).texture.bind(gl);
				}
				
				BalboaTexture texture = engine.textures.get(current_texture);
				
			    gl.glTexCoord2f(texture.left,texture.bottom);
				gl.glNormal3f((float)normals[z][x].x,(float)normals[z][x].y,(float)normals[z][x].z);
				gl.glVertex3f((float)(x*cell_size),(float)verticies[z][x],(float)(z*cell_size));
				
				gl.glTexCoord2f(texture.left,texture.top);
				gl.glNormal3f((float)normals[z+1][x].x,(float)normals[z+1][x].y,(float)normals[z+1][x].z);
				gl.glVertex3f((float)(x*cell_size),(float)verticies[z+1][x],(float)((z+1)*cell_size));
				
				gl.glTexCoord2f(texture.right,texture.top);
				gl.glNormal3f((float)normals[z+1][x+1].x,(float)normals[z+1][x+1].y,(float)normals[z+1][x+1].z);
				gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z+1][x+1],(float)((z+1)*cell_size));
				
				gl.glTexCoord2f(texture.right,texture.bottom);
				gl.glNormal3f((float)normals[z][x+1].x,(float)normals[z][x+1].y,(float)normals[z][x+1].z);
				gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z][x+1],(float)(z*cell_size));
			}
			gl.glEnd();
		}
		gl.glDisable(GL2.GL_TEXTURE_2D);
		/*Grid
		if(engine.display_grid)
		{
			gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_LINE);
			gl.glLineWidth(1f);
			for(int z = 0; z < textures.length; z++)
			{
				gl.glBegin(GL2.GL_QUADS);
				for(int x = 0; x < textures[0].length; x++)
				{
				    gl.glColor3f((float)0.f,(float)0.f,(float)0.f);
					gl.glNormal3f((float)normals[z][x].x,(float)normals[z][x].y,(float)normals[z][x].z);
					gl.glVertex3f((float)(x*cell_size),(float)verticies[z][x],(float)(z*cell_size));
					gl.glNormal3f((float)normals[z+1][x].x,(float)normals[z+1][x].y,(float)normals[z+1][x].z);
					gl.glVertex3f((float)(x*cell_size),(float)verticies[z+1][x],(float)((z+1)*cell_size));
					gl.glNormal3f((float)normals[z+1][x+1].x,(float)normals[z+1][x+1].y,(float)normals[z+1][x+1].z);
					gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z+1][x+1],(float)((z+1)*cell_size));
					gl.glNormal3f((float)normals[z][x+1].x,(float)normals[z][x+1].y,(float)normals[z][x+1].z);
					gl.glVertex3f((float)((x+1)*cell_size),(float)verticies[z][x+1],(float)(z*cell_size));
				}
				gl.glEnd();
			}
		}*/
	}
}

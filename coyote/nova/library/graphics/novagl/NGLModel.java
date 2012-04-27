package nova.library.graphics.novagl;

import java.util.HashMap;
import java.util.LinkedList;

import nova.library.GLOBALS;
import nova.library.graphics.textures.NGLTexture;
import nova.library.utilities.Parser;
import nova.library.graphics.vecmath.Face3d;
import nova.library.graphics.vecmath.Vertex3d;
import nova.library.xml.XMLElementList;

public class NGLModel
{
	public String name;
	public String geometry;
	public NGLGroup groups[];
	//public Face3d faces[];	
	public NGLMaterial materials[];
	
	public NGLModel(String name, String geometry,HashMap<String,NGLTexture> texturemaps)
	{
		this.name = name;
		this.geometry = geometry;
		load(geometry,texturemaps,.1);
	}
	public void load(String geometry,HashMap<String,NGLTexture> texturemaps,double scale)
	{
		LinkedList<NGLGroup> groups = new LinkedList<NGLGroup>();
		LinkedList<Face3d> faces = new LinkedList<Face3d>();
		LinkedList<Vertex3d> verticies = new LinkedList<Vertex3d>();
		LinkedList<Vertex3d> normals = new LinkedList<Vertex3d>();
		LinkedList<Vertex3d> textures = new LinkedList<Vertex3d>();
		
		for(String line: Parser.fileToList(geometry,"#"))
		{
			String str[] = line.split("\\s+");
			if(str[0].equals("v"))
				verticies.add(new Vertex3d(scale*Double.parseDouble(str[1]),scale*Double.parseDouble(str[2]),scale*Double.parseDouble(str[3])));
			else if(str[0].equals("vn"))
				normals.add(new Vertex3d(Double.parseDouble(str[1]),Double.parseDouble(str[2]),Double.parseDouble(str[3])));
			else if(str[0].equals("vt"))
				textures.add(new Vertex3d(Double.parseDouble(str[1]),Double.parseDouble(str[2]),0.0));
		}
		NGLGroup current_group = null;
		for(String line: Parser.fileToList(geometry,"#"))
		{
			String str[] = line.split("\\s+");
			if(str[0].equals("g"))
			{
				if(current_group!=null)
					current_group.faces = faces.toArray(new Face3d[]{});
				current_group = new NGLGroup(str[1],texturemaps.get(str[1]));
				groups.add(current_group);
			}
			else if(str[0].equals("f"))
			{
				//System.err.println("Creating Face3D for "+line +" with "+(str.length-1)+" verticies");
				Face3d face = new Face3d(str.length-1);
				for(int i = 1; i < str.length; i++)
				{
					String values[] = str[i].split("/");
					if(values.length==1)
						face.verticies[i-1]= verticies.get(Integer.parseInt(values[0])-1);//To move to zero-based;
					else
					{
						face.verticies[i-1] = verticies.get(Integer.parseInt(values[0])-1);//To move to zero-based;
						face.textures[i-1] = (values[1].equals(""))?null:textures.get(Integer.parseInt(values[1])-1);//To move to zero-based;
						face.normals[i-1] = (values[2].equals(""))?null:normals.get(Integer.parseInt(values[2])-1);//To move to zero-based;
					}
				}
				faces.add(face);
			}
		}
		if(current_group!=null)
			current_group.faces = faces.toArray(new Face3d[]{});
		this.groups =  groups.toArray(new NGLGroup[]{});
	}

	public String toString()
	{
		String str = "";
		/*for(Vertex3d vert: verticies)
		{
			str += "X,Y,Z = "+vert.x+"\t"+vert.y+"\t"+vert.z+"\n";
		}*/
		return str;
	}
}

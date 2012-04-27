package nova.library.graphics.vecmath;

import java.util.LinkedList;

public class Face3d
{
	public Vertex3d verticies[];
	public Vertex3d textures[];
	public Vertex3d normals[];
	public Face3d(int num)
	{
		this.verticies = new Vertex3d[num];
		this.textures = new Vertex3d[num];
		this.normals = new Vertex3d[num];
	}
}

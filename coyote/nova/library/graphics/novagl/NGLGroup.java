package nova.library.graphics.novagl;

import nova.library.graphics.textures.NGLTexture;
import nova.library.vecmath.Face3d;

public class NGLGroup
{
	public String name;
	public NGLTexture texture;
	public Face3d faces[];
	public NGLGroup(String name,NGLTexture texture)
	{
		this.name = name;
		this.texture = texture;
	}
}

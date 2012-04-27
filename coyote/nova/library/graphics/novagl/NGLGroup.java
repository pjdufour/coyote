package nova.library.graphics.novagl;

import nova.library.graphics.textures.BalboaTexture;
import nova.library.graphics.vecmath.Face3d;

public class NGLGroup
{
	public String name;
	public BalboaTexture texture;
	public Face3d faces[];
	public NGLGroup(String name,BalboaTexture texture)
	{
		this.name = name;
		this.texture = texture;
	}
}

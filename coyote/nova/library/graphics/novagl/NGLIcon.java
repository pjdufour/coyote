package nova.library.graphics.novagl;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import nova.library.core.Game;
import nova.library.core.Settings;

public class NGLIcon
{	
	private Game game;
	
	private String name;
	private String icon_filename;
	
	private Color color_normal;
	private Color color_selected;
	private Color color_hover;
	
	private Texture texture_normal;
	private Texture texture_selected;
	private Texture texture_hover;
	
	public NGLIcon(Game game,String name,String icon_filename)
	{
		this.game = game;
		this.name = name;
		this.icon_filename = icon_filename;
	}
	public void compile()
	{
		try{
		Settings settings = game.getSettings();
		color_normal = settings.getColor(Settings.UI_COLOR_TOOL_BACKGROUND);
		color_selected = settings.getColor(Settings.UI_COLOR_TOOL_SELECTED);
		color_hover = settings.getColor(Settings.UI_COLOR_TOOL_HOVER);
			
		int width = 30;
		ByteArrayOutputStream os;
		//Icon Image
		BufferedImage image_icon = ImageIO.read(new File(icon_filename));
		for(int y = 0; y < width; y++)
		{
			for(int x = 0; x < width; x++)
			{
				if(y==0||y==width-1||x==0||x==width-1)
				{
					image_icon.setRGB(x,width-y-1,Color.BLACK.getRGB());
				}
			}
		}
		//Normal Image
		os = new ByteArrayOutputStream();
		ImageIO.write(createImage(width,color_normal,image_icon),"png",os); 
		texture_normal = TextureIO.newTexture(new ByteArrayInputStream(os.toByteArray()),true,"png");
		//Selected Image
		os = new ByteArrayOutputStream();
		ImageIO.write(createImage(width,color_selected,image_icon),"png",os); 
		texture_selected = TextureIO.newTexture(new ByteArrayInputStream(os.toByteArray()),true,"png");
		//Hover Image
		os = new ByteArrayOutputStream();
		ImageIO.write(createImage(width,color_hover,image_icon),"png",os); 
		texture_hover = TextureIO.newTexture(new ByteArrayInputStream(os.toByteArray()),true,"png");
		}catch(IOException e){e.printStackTrace();System.exit(-1);}
	}
	private BufferedImage createImage(int width,Color background,BufferedImage foreground)
	{
		BufferedImage image = new BufferedImage(width,width,BufferedImage.TYPE_INT_ARGB);
		for(int y = 0; y < width; y++)
		{
			for(int x = 0; x < width; x++)
			{
				image.setRGB(x,width-y-1,background.getRGB());
			}
		}
		image.getGraphics().drawImage(foreground,0,0,30,30,null);
		return image;
	}
	
	public void bindNormal(GL2 gl)
	{
		texture_normal.bind(gl);
	}
	public void bindSelected(GL2 gl)
	{
		texture_selected.bind(gl);
	}
	public void bindHover(GL2 gl)
	{
		texture_hover.bind(gl);
	}
	public void enableNormal(GL2 gl)
	{
		texture_normal.enable(gl);
	}
	public void enableSelected(GL2 gl)
	{
		texture_selected.enable(gl);
	}
	public void enableHover(GL2 gl)
	{
		texture_hover.enable(gl);
	}
}

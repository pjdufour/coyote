package nova.library.graphics.novagl;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;

public class NovaCursorManager
{
	public static final int TYPE_TRANSPARENT = 1;
	public static final int TYPE_POINTER = 2;
	public static final int TYPE_HAND = 3;
	public static final int TYPE_CROSSHAIR = 4;
	public static final int TYPE_BUSY = 5;
	public static final int TYPE_RETICLE = 6;
	
	private Cursor cursor_transparent;
	private Cursor cursor_pointer;
	private Cursor cursor_hand;
	private Cursor cursor_crosshair;
	private Cursor cursor_busy;
	private Cursor cursor_reticle;
	public NovaCursorManager()
	{
		
	}
	public Cursor getCursor(int type)
	{
		if(type==TYPE_TRANSPARENT)
		{
			if(cursor_transparent==null)
			{
				int[] pixels = new int[16*16];
				//for(int i = 0; i < pixels.length; i++)
				//	pixels[i] = 0xAAAAAAA;
				Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16,16,pixels,0,16));
				cursor_transparent = Toolkit.getDefaultToolkit().createCustomCursor(image,new Point(0,0),"invisibleCursor");
			}
			return cursor_transparent;
		}
		else if(type==TYPE_RETICLE)
		{
			if(cursor_reticle==null)
			{
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
				Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16,16,pixels,0,16));
				cursor_reticle = Toolkit.getDefaultToolkit().createCustomCursor(image,new Point(0,0),"reticleCursor");
			}
			return cursor_reticle;
		}
		else if(type==TYPE_POINTER)
		{
			if(cursor_pointer==null)
				cursor_pointer = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR); 
			return cursor_pointer;
		}
		else if(type==TYPE_HAND)
		{
			if(cursor_hand==null)
				cursor_hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
			return cursor_hand;
		}
		else if(type==TYPE_CROSSHAIR)
		{
			if(cursor_crosshair==null)
				cursor_crosshair =Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); 
			return cursor_crosshair;
		}
		else if(type==TYPE_BUSY)
		{
			if(cursor_busy==null)
				cursor_busy = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			return cursor_busy;
		}
		else
			return null;
	}
}

package nova.coyote.examples;

import java.util.LinkedHashMap;

import nova.game.Balboa;
import nova.game.Coyote;
import nova.game.Tecolote;
import nova.library.graphics.textures.BalboaTexture;
import nova.library.graphics.textures.BalboaTextureLoader;
import nova.library.logs.Logs;
import nova.library.settings.Settings;

public class Example
	implements Coyote, Balboa, Tecolote
{

	private Settings settings;
	private Logs logs; 
	private BalboaTextureLoader loader;
	private LinkedHashMap<Integer,BalboaTexture> textures;
	
	public Example(String []args)
	{
		try
		{
			settings = new Settings(this,"nova/balboa/examples/settings.xml");
			logs = new Logs(this);
			settings.printSettingsToConsole();
			loader = new BalboaTextureLoader(this);
			textures = loader.load(settings.getString(Settings.FILE_TEXTURES));
			for(int textureID: textures.keySet())
			{
				BalboaTexture texture = textures.get(textureID);
				texture.compile();
				
			}
			
		}catch(Exception e){e.printStackTrace();System.exit(-1);}
	}
	@Override
	public Logs getLogs()
	{
		return logs;
	}
	@Override
	public Settings getSettings()
	{
		return settings;
	}
	public static void main(String []args)
	{
		new Example(args);
	}
}

package nova.library.graphics.novagl;

import nova.game.Coyote;
import nova.library.settings.Settings;

public class HUD
{
	private Coyote coyote;
	private NGLCanvas canvas;
	
	private int map[][];//Values Link to Actions
	private Action actions[];
	public NGLTool tools[];
	
	public int currentTool;
	public int hover;
	public int selected;
	
	public HUD(Coyote coyote)
	{
		this.coyote = coyote;
	}
	public void activate(int width,int height)
	{
		Engine engine = coyote.getEngine();
		Settings settings = coyote.getSettings();
		map = new int[height][width];
		for(int i = 0; i < height; i++)
		{
			for(int j = 0; j < width; j++)
			{
				map[i][j] = -1;
			}
		}
		tools = new NGLTool[1];
		tools[0] = new NGLTool(engine.icons.get(0),new NGLIcon[]{engine.icons.get(1)});
		
		actions = new Action[1];
		actions[0] = new ToolAction(0);
		
		currentTool = -1;
		selected = -1;
		hover = -1;
		
		//Build Map
		int x = 30;
		int y = (int)(height - 90);
		for(int i = 0; i < tools.length; i++)
		{
			for(int a = 0; a < 30; a++)
			{
				for(int b = 0; b < 30; b++)
				{
					map[y+a][x+b] = i;
				}
			}
			y -=60;
		}
	}
	public void deactivate()
	{
		map = null;
		actions = null;
		currentTool = -1;
		tools = null;
		hover = -1;
		selected = -1;
	}

	public boolean pressed(int x,int y)
	{
		if(map[y][x]!=-1)
		{
			actions[map[y][x]].pressed();
			return true;
		}
		else
			return false;
	}
	public boolean released(int x,int y)
	{
		if(map[y][x]!=-1)
		{
			actions[map[y][x]].released();
			return true;
		}
		else
			return false;
	}
	private abstract class Action
	{
		protected int id;
		public Action(int id)
		{
			this.id = id;
		}
		public abstract void pressed();
		public abstract void released();
	}
	private class ToolAction extends Action
	{
		public ToolAction(int id)
		{
			super(id);
		}

		public void pressed()
		{
			currentTool = (currentTool==id)?-1:id;
		}
		public void released()
		{
		}
	}
}

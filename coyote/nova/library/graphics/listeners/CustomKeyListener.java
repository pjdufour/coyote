package nova.library.graphics.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.LinkedList;

import nova.library.core.Command;

public class CustomKeyListener implements KeyListener
{
	private HashMap<Integer,LinkedList<Command>> pressed_bindings;
	private HashMap<Integer,LinkedList<Command>> released_bindings;
	private HashMap<Integer,LinkedList<Command>> typed_bindings;
	public CustomKeyListener()
	{
		pressed_bindings = new HashMap<Integer,LinkedList<Command>>();
		released_bindings = new HashMap<Integer,LinkedList<Command>>();
		typed_bindings = new HashMap<Integer,LinkedList<Command>>();
	}
	public void keyPressed(KeyEvent e)
	{
		if(pressed_bindings.containsKey(e.getKeyCode()))
		{
			for(Command effect: pressed_bindings.get(e.getKeyCode()))
				effect.execute();
		}
	}
	public void keyReleased(KeyEvent e)
	{
		if(released_bindings.containsKey(e.getKeyCode()))
		{
			for(Command effect: released_bindings.get(e.getKeyCode()))
				effect.execute();
		}
	}
	public void keyTyped(KeyEvent e)
	{
		if(typed_bindings.containsKey(e.getKeyCode()))
		{
			for(Command effect: typed_bindings.get(e.getKeyCode()))
				effect.execute();
		}
	}
	public void addKeyBinding(int type,int code,Command command)
	{
		if(type==KeyEvent.KEY_PRESSED)
		{
			if(!pressed_bindings.containsKey(code)) pressed_bindings.put(code,new LinkedList<Command>());
			pressed_bindings.get(code).add(command);
		}
		else if(type==KeyEvent.KEY_RELEASED)
		{
			if(!released_bindings.containsKey(code)) released_bindings.put(code,new LinkedList<Command>());
			released_bindings.get(code).add(command);
		}
		else if(type==KeyEvent.KEY_TYPED)
		{
			if(!typed_bindings.containsKey(code)) typed_bindings.put(code,new LinkedList<Command>());
			typed_bindings.get(code).add(command);
		}
	}
}

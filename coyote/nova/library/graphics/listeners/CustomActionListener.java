package nova.library.graphics.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;

import nova.library.core.Command;

public class CustomActionListener implements ActionListener
{
	private HashMap<String,LinkedList<Command>> bindings;
	public CustomActionListener()
	{
		bindings = new HashMap<String,LinkedList<Command>>();
	}
	public void addActionBinding(String strActionCommand,Command command)
	{
		if(!bindings.containsKey(strActionCommand))
			bindings.put(strActionCommand,new LinkedList<Command>());
		bindings.get(strActionCommand).add(command);
	}
	public void actionPerformed(ActionEvent e)
	{
		if(bindings.containsKey(e.getActionCommand()))
		{
			for(Command command: bindings.get(e.getActionCommand()))
				command.execute();
		}
	}
}

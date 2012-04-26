package nova.library.graphics.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;

import nova.library.core.Command;

public class SimpleActionListener implements ActionListener
{
	private LinkedList<Command> commands;
	public SimpleActionListener()
	{
		commands = new LinkedList<Command>();
	}
	public void addCommand(Command command)
	{
		commands.add(command);
	}
	public void actionPerformed(ActionEvent e)
	{
		for(Command command: commands)
			command.execute();
	}
}

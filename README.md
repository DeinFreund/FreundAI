FreundAI
========

My first try on a zero-k/spring AI using the JavaOOInterface. 
Many hardcoded unitnames and actions. 
Partially object oriented.

This is a NetBeans project folder.
To simply test the AI you can replace the SkirmishAI.jar file in Spring/AI/Skirmish/NullOOJavaAI/0.1 by this project's compiled jar file.
Then open AIInfo.lua and change the key 'className' to 'zkai.zkai'. It should look like this:
	{
		key    = 'className',
		value  = 'zkai.zkai',
		desc   = 'fully qualified name of a class that implements interface com.springrts.ai.AI',
	},

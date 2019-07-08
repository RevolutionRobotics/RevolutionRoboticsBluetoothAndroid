# Robotics Blockly Code Editor SDK

## Introduction

**Robotics Blockly** is built on Google’s open-source Blockly library, which represents coding concepts as interlocking blocks, and transforms these blocks into syntactically correct source code.

The Robotics Blockly Library allows you to embed the Blockly coding interface customised specially for Revolution Robotics’ robots. It consists many custom blocks to enable you to program the robots and utilise all their features.

## `BlocklyView`

The core of the Robotics Blockly Android SDK is the `BlocklyView` view class, which encapsulates all the features of the blockly editor. Add this view to your layout to embed the code editor.

`BlocklyView` provides some features to manipulate its contents:

- `saveProgram`: saves the contents of the workspace. The following three outputs are generated, all of which are passed to the listener’s corresponding method:

    - **XML**: the XML generated represents the current state of the blockly workspace. This can be used to reload programs into the workspace later.

    - **Python**: a python file is generated, which holds the syntactically correct source code of the program assembled in the editor. This python code can be uploaded and run on the robot.

    - **Variables**: blockly also exports all the variables used as ports in the source, so you can determine which programs are compatible with which configurations (the configuration should have all the variable names as port names defined to be considered compatible).

- `loadProgram`: loads the contents of a blockly XML file into the workspace.

- `clearWorkspace`: removes all blocks from the workspace.

To use `BlocklyView`, simply add it to your layout and initialize it by calling its `init` method, supplying a `DialogFactory` instance (see below).


##`DialogFactory`

Many Blockly features, mostly blocks, require input from the user. Communications are done through dialogs, and all dialogs come with an implementation of the `BlocklyResult` abstact class. The dialogs provide the UI, while responses are handled by the `BlocklyResult` implementations. For example, when creating a variable, Blockly raises a text input dialog, so the user can provide its name, and the provided name is passed back to the editor using the `VariableResult` instance, which is a direct subclass of `BlocklyResult`.

All dialogs have corresponding `BlocklyResult` implementations.

To make the dialog’s appearance fully customizable, Robotics Blockly only provides an interface called `DialogFactory`, through which these dialogs should appear.

As mentioned before, for all dialogs to be displayed, the `DialogFactory` receives a `BlocklyResult` implementation, that enables you to provide the result to the `BlocklyView` blockly editor. The `BlocklyResult` implementations provide various methods to do so:

- `cancel`: cancels the input, as if the user decided not to provide any inputs.
- `confirm*`: confirms the input, as if the user supplied the result.

*\*the concrete method naming can differ; to give a few examples, it can be “confirmDelete”, “confirmDuplicate”, “confirmIndex”, etc. However, all the method names start with “confirm” for easier discoverability.*

Either one of these methods **must** be called - `BlocklyView` is blocked until one of them is called.

`DialogFactory` has the following methods to display modals:

- `showAlertDialog`
Display an alert for the user with a message.

- `showConfirmationDialog`
Let the user confirm an action with a message. 

- `showDirectionSelectorDialog`
Show a direction selector modal for the user. Comes with a default selection if applicable.

- `showSlider`
Show a modal with a slider so the user can input a number. Comes with a default and a maximum value if applicable.

- `showDialpad`
Show a dialpad modal so the user can input floating point values. Comes with a default value if applicable.

- `showColorPicker`
Show a color picker for the user with the given color set. Comes with a default value if applicable.

- `showSoundPicker`
Show a sound picker for the user. This will display all the sound files available on the robot. Comes with a default value if applicable.

- `showTextInput`
Show a text input for the user. Comes with a default value if applicable.

- `showDonutSelector`
Show a led selector for the user, preferably in a round shape mimicking the LED display on the robot (hence the name, “DonutSelector”). There are two types of LED selection, single (where the user can select a single LED) and multi (where the user can select any number of LEDs). Comes with a default selection if applicable.

- `showOptionSelector`
Show custom options for the user. The available options come in an array, which has 2 to 6 options. Comes with a default selection if applicable.


- `showBlockOptionsDialog`
Show the block options dialog for the user, where the user can add a comment to the given block, delete it, duplicate it, or get help about it.


- `showVariableOptionsDialog`
Show the variable options modal for the user, where the user can switch the variable to another one, or delete a variable.



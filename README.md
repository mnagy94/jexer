Jexer - Java Text User Interface library
========================================

This library implements a text-based windowing system loosely
reminiscent of Borland's [Turbo
Vision](http://en.wikipedia.org/wiki/Turbo_Vision) system.  It looks
like this:

![WezTerm, translucent images](/screenshots/wezterm_translucent_images.png?raw=true "WezTerm, translucent images")

...or this:

![Terminal, Image, Table](/screenshots/new_demo1.png?raw=true "Terminal, Image, Table")

...or anything in between.  Translucent windows -- including layered
images -- are supported and generally look as one would expect in a
modern graphical environment...but it's mostly text.  Translucent
windows were inspired in part by
[notcurses](https://github.com/dankamongmen/notcurses).

Jexer works on both Xterm-like terminals and Swing, and supports
images in both Xterm and Swing.  On Swing, images are true color; on
Xterm, images are rendered as sixel, iTerm2, or Jexer images.

Support for pixel-based operations was introduced in version 1.5.0.
If the terminal supports mouse mode 1016 (SGR-Pixel), one can now get
smooth(er) mouse motion with custom bitmap overlaid mouse.  Below is
stock xterm, with a custom mouse icon, and SGR-Pixel mode active:

![Xterm SGR-Pixel Mouse](/screenshots/xterm_pixel_mouse.gif?raw=true "Xterm SGR-Pixel Mouse")

A new sixel encoder was introduced in version 1.6.0, and looks and
performs much better.  This encoder was inspired in part by
[chafa's](https://hpjansson.org/chafa/) high-performance principal
component analysis based sixel encoder.

![PCA color matching with 128-color palette and translucent windows](/screenshots/pca_match.png?raw=true "PCA color matching with 128-color palette and translucent windows")

Jexer can be run inside its own terminal window, with support for all
of its features including images and mouse, and more terminals:

![Yo Dawg...](/screenshots/jexer_sixel_in_sixel.png?raw=true "Yo Dawg, I heard you like text windowing systems, so I ran a text windowing system inside your text windowing system so you can have a terminal in your terminal.")



How...?  What...?
-----------------

Wondering how I did it?  [Here you
go.](https://jexer.sourceforge.io/evolution.html)



License
-------

Jexer is available to all under the MIT License.  See the file LICENSE
for the full license text.



Obtaining Jexer
---------------

Jexer is available on Maven Central:

```xml
<dependency>
  <groupId>com.gitlab.klamonte</groupId>
  <artifactId>jexer</artifactId>
  <version>1.6.0</version>
</dependency>
```

Binary releases are available on SourceForge:
https://sourceforge.net/projects/jexer/files/jexer/

The Jexer source code is hosted at:
https://gitlab.com/AutumnMeowMeow/jexer


Why I Do Not Post My Projects Anymore
-------------------------------------

[Why I Do Not Post My Projects
Anymore](https://gitlab.com/AutumnMeowMeow/jexer/wikis/no-release-announcements)


Documentation
-------------

* [Wiki](https://gitlab.com/AutumnMeowMeow/jexer/wikis/home)

* [Jexer web page](https://jexer.sourceforge.io/)

* [Java API Docs](https://jexer.sourceforge.io/apidocs/api/index.html)

* [Development
  Standards](https://gitlab.com/AutumnMeowMeow/jexer/wikis/dev-standards)

* [Porting Guide](https://gitlab.com/AutumnMeowMeow/jexer/wikis/porting) -
  If you don't like writing Java, here is your map to where the key
  features are so that you can implement them in a different
  system/language.



Programming Examples
--------------------

See [Xterm Window Manager](https://xtermwm.sourceforge.io) for a more
comprehensive demonstration of what Jexer can accomplish.  Here one
can see a floating terminal window over tiled terminals, two of which
are showing images:

![Floating terminal over tiled terminals](/screenshots/floating_terminal.png?raw=true "Floating terminal over tiled terminals")

Jexer's examples/ folder currently contains:

  * A [prototype tiling window
    manager](/examples/JexerTilingWindowManager.java) in less than 250
    lines of code.

  * A much slicker [prototype tiling window
    manager](/examples/JexerTilingWindowManager2.java) in less than 200
    lines of code.

  * A [prototype image thumbnail
    viewer](/examples/JexerImageViewer.java) in less than 350 lines of
    code.

  * A [prototype Xterm video player (using JavaCV to decode video
    frames)](/examples/XtermVideoPlayer.java) in less than 200 lines
    of code.

jexer.demos contains official demos showing all of the existing UI
controls.  The demos can be run as follows:

  * 'java -jar jexer.jar' .  This will use System.in/out with
    Xterm-like sequences on non-Windows non-Mac platforms.  On Windows
    and Mac it will use a Swing JFrame.

  * 'java -Djexer.Swing=true -jar jexer.jar' .  This will always use
    Swing on any platform.

  * 'java -cp jexer.jar jexer.demos.Demo2 PORT' (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server that will update with screen size changes.

  * 'java -cp jexer.jar jexer.demos.Demo3' .  This will use
    System.in/out with Xterm-like sequences.  One can see in the code
    how to pass a different InputReader and OutputReader to
    TApplication, permitting a different encoding than UTF-8; in this
    case, code page 437.

  * 'java -cp jexer.jar jexer.demos.Demo4' .  This demonstrates hidden
    windows and a custom TDesktop.

  * 'java -cp jexer.jar jexer.demos.Demo5' .  This demonstrates two
    demo applications using different fonts in the same Swing frame.

  * 'java -cp jexer.jar jexer.demos.Demo6' .  This demonstrates two
    applications performing I/O across three screens: an Xterm screen
    and Swing screen, monitored from a third Swing screen.

  * 'java -cp jexer.jar jexer.demos.Demo7' .  This demonstrates the
    BoxLayoutManager, achieving a similar result as the
    javax.swing.BoxLayout apidocs example.

  * 'java -cp jexer.jar jexer.demos.Demo8 PORT' (where PORT is a
    number to run the TCP daemon on).  This will use the Xterm backend
    on a telnet server to share one screen to many terminals.



Terminal Support
----------------

Most popular terminals can run Jexer, but only a few support all of
Jexer's features.  Jexer is actively developed against
[xterm](https://invisible-island.net/xterm/) and
[wezterm](https://wezfurlong.org/wezterm/) .  The table below lists
the terminals last tested against Jexer:

| Terminal       | Environment        | Mouse Click | Mouse Cursor | Images |
| -------------- | ------------------ | ----------- | ------------ | ------ |
| xterm          | X11                | yes         | yes          | yes    |
| jexer          | CLI, X11, Windows  | yes         | yes          | yes    |
| wezterm        | X11, Windows       | yes         | yes          | yes(7) |
| foot(3)        | Wayland            | yes         | yes          | yes    |
| contour(3)     | X11                | yes         | yes          | yes    |
| mintty         | Windows            | yes         | yes          | yes    |
| mlterm         | X11                | yes         | yes          | yes    |
| RLogin         | Windows            | yes         | yes          | yes    |
| xterm.js(8)    | Web                | yes         | yes          | yes    |
| alacritty(3b)  | X11                | yes         | yes          | yes    |
| gnome-terminal | X11                | yes         | yes          | no     |
| iTerm2         | Mac                | yes         | yes          | no(5)  |
| kitty(3)       | X11                | yes         | yes          | no(9)  |
| lcxterm        | CLI, Linux console | yes         | yes          | no     |
| rxvt-unicode   | X11                | yes         | yes          | no(2)  |
| xfce4-terminal | X11                | yes         | yes          | no     |
| Windows Terminal(6) | Windows       | yes         | yes          | no     |
| DomTerm(3)     | Web                | yes         | no           | yes    |
| darktile       | X11                | yes         | no           | no(5)  |
| konsole        | X11                | yes         | no           | no     |
| yakuake        | X11                | yes         | no           | no     |
| screen         | CLI                | yes(1)      | yes(1)       | no(2)  |
| tmux           | CLI                | yes(1)      | yes(1)       | no     |
| putty          | X11, Windows       | yes         | no           | no(2)  |
| qodem(3)       | CLI, Linux console | yes         | yes(4)       | no     |
| qodem-x11(3)   | X11                | yes         | no           | no     |
| yaft           | Linux console (FB) | no          | no           | yes    |
| Linux          | Linux console      | no          | no           | no(2)  |
| MacTerm        | Mac                | no          | no           | no(2)  |

1 - Requires mouse support from host terminal.

2 - Also fails to filter out sixel data, leaving garbage on screen.

3 - Latest in repository.

3b - Latest in repository, using graphics PR branch.

4 - Requires TERM=xterm-1003 before starting.

5 - Sixel images can crash terminal.

6 - Version 1.4.3243.0, on Windows 10.0.19041.1.  Tested against
    WSL-1 Debian instance.

7 - Both sixel and iTerm2 images.

8 - Using jerch's xterm-addon-image.

9 - Kitty image protocol support will never be in Jexer, as a matter
    of principle.  Should that terminal adopt iTerm2 or sixel, then
    images could work.  (Image support for kitty was removed on
    February 10, 2022.)

See Also
--------

* [Xterm Window Manager](https://gitlab.com/AutumnMeowMeow/xtermwm) is
  a text-based window manager.  It has virtual desktops, tiled
  terminals with draggable resizing, cascading terminal windows, and a
  plugin system for adding functionality.  Add LCXterm and one can
  have a mouse-supporting X11-like text-based "GUI" on the raw Linux
  console.

* [XtermDOOM](https://gitlab.com/AutumnMeowMeow/xtermdoom) is an
  effort to make [MochaDoom](https://github.com/jendave/mochadoom) run
  smoothly under Xterm.  The game is not yet playable, but it can show
  off Jexer's multiplexing and multihead image capabilities.  For
  ramblings about the game, terminal resources, etc. see
  [/r/xtermdoom](https://reddit.com/r/xtermdoom).

* [LCXterm](https://lcxterm.sourceforge.io) is a curses-based terminal
  emulator that allows one to use Jexer with full support on the raw
  Linux console.

* [ptypipe](https://gitlab.com/AutumnMeowMeow/ptypipe) is a small C
  utility that permits a Jexer TTerminalWindow to resize the running
  shell when its window is resized.

* [Tranquil Java IDE](https://tjide.sourceforge.io) is a TUI-based
  integrated development environment for the Java language that was
  built using a very lightly modified GPL version of Jexer.



Acknowledgements
----------------

Jexer makes use of the Terminus TrueType font [made available
here](http://files.ax86.net/terminus-ttf/) .

Jexer incorporates some ideas done first, or better implemented,
elsewhere.  If you like what you see here, please take a look at these
projects too:

* [notcurses](https://github.com/dankamongmen/notcurses)

* [chafa](https://hpjansson.org/chafa/)

* [wezterm](https://wezfurlong.org/wezterm/)

* [vtm](https://github.com/netxs-group/vtm)

💖

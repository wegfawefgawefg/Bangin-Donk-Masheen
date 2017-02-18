import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.sound.midi.*;


public class BeatMachine implements ItemListener
{
	//	-	-	-	-	-	MUSIC VARIABLES	-	-	-	-	-	//
	public static final int NUM_CHANNELS = 16;
	public static final int DEFAULT_BEATS_PER_MINUTE = 60;
	public static final int BEATS_PER_CYCLE = 50;
	public static final int CHANGE_INSTRUMENT = 192;
	public static final int NOTE_ON = 144;
	public static final int NOTE_OFF = 128;	
	public static final int NOTE_LENGTH = 1;
	public static final int PULSES_PER_QUARTER_NOTE = 1;
	public static final double TEMPO_CHANGE_FACTOR = 0.05;

	int beatsPerMinute = DEFAULT_BEATS_PER_MINUTE;
	boolean isPlaying = false;

	Sequencer sequencer = null;
	Sequence sequence = null;

	//	tracks for instrument changes
	ArrayList<Track> instrumentChangeTracks = new ArrayList<Track>();

	//	one track per channel
	ArrayList<Track> channelTracks = new ArrayList<Track>();

	//	initial instruments of each channel
	static final int[] defaultInstruments = {	50,
												55,
												60,
												65,
												70,
												75,
												80,
												85,
												90,
												95,
												100,
												105,
												110,
												115,
												120,
												125	};
	//	initial notes of each channel
	static final int[] defaultNotes = {			50,
												55,
												60,
												65,
												70,
												75,
												80,
												85,
												90,
												95,
												100,
												105,
												110,
												115,
												120,
												125	};

	//	instruments of each channel
	ArrayList<Integer> instrumentOfChannel;
	{
		instrumentOfChannel = new ArrayList<Integer>();
		for( int instrumentNum : defaultInstruments )
		{
			instrumentOfChannel.add( instrumentNum );
		}
	}

	//	notes of each channel
	ArrayList<Integer> noteOfChannel;
	{
		noteOfChannel = new ArrayList<Integer>();
		for( int noteNum : defaultNotes )
		{
			noteOfChannel.add( noteNum );
		}
	}


	//	-	-	-	-	-	GUI VARIABLES	-	-	-	-	-	//
	public static final int WINDOW_HEIGHT = 400;
	public static final int WINDOW_WIDTH = 1000;

	//	main jframe
	JFrame frame;

	//	//	main jpanel and its layout
	JPanel background;
	BorderLayout backgroundLayout;

	//	layout manager for the east and west panels respectively
	Box buttonBox;
	Box labelBox;

	//	center panel and its layout
	JPanel centerPanel;
	GridLayout grid;

	//	rows of checkboxes
	ArrayList< ArrayList<JCheckBox> > checkBoxes= new ArrayList< ArrayList<JCheckBox> >();

	//	buttons
	JButton buttonStart;
	JButton buttonPause;
	JButton buttonStop;
	JButton buttonRewind;
	JButton buttonReset;
	JButton buttonIncreaseTempo;
	JButton buttonDecreaseTempo;

	//	initial labels of each channel
	static final String[] defaultChannelLabels = {	"Donk1",
													"Donk2",
													"Donk3",
													"Donk4",
													"Donk5",
													"Donk6",
													"Donk7",
													"Donk8",
													"Donk9",
													"Donk10",
													"Donk11",
													"Donk12",
													"Donk13",
													"Donk14",
													"Donk15",
													"Donk16",	};

	//	labels of each channel
	ArrayList<Label> labelOfChannel;
	{
		labelOfChannel = new ArrayList<Label>();
		for( String labelName : defaultChannelLabels )
		{
			labelOfChannel.add( new Label( labelName ) );
		}
	}

	//	-	-	-	-	-	FUNCTIONS	-	-	-	-	-	//
	public static void main( String[] args )
	{
		BeatMachine beatMachine = new BeatMachine();
		
		//	initialize the GUI
		beatMachine.initGUI();

		//	initialize the midi stuff
		beatMachine.initMidi();
	}


	//	-	-	-	-	-	GUI FUNCTIONS	-	-	-	-	-	//
	//	init the GUI
	public void initGUI( )
	{
		frame = new JFrame( "Bangin' Donk Masheen" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		backgroundLayout = new BorderLayout();
		background = new JPanel( backgroundLayout );
		background.setBorder( BorderFactory.createEmptyBorder( 10,10,10,10 ) );

		//	right box
		buttonBox = new Box( BoxLayout.Y_AXIS );

		//	add the buttons
		buttonStart	= new JButton( "Start" );
		buttonBox.add( buttonStart );

		buttonStop = new JButton( "Stop" );
		buttonBox.add( buttonStop );

		buttonPause = new JButton( "Pause" );
		buttonBox.add( buttonPause );

		buttonRewind = new JButton( "Rewind" );
		buttonBox.add( buttonRewind );

		buttonReset = new JButton( "Reset" );
		buttonBox.add( buttonReset );

		buttonIncreaseTempo = new JButton( "Increase Tempo" );
		buttonBox.add( buttonIncreaseTempo );

		buttonDecreaseTempo = new JButton( "Decrease Tempo" );
		buttonBox.add( buttonDecreaseTempo );

		//	left box
		labelBox = new Box( BoxLayout.Y_AXIS );

		//	add the initial labels to the label box
		for( Label label : labelOfChannel )
		{
			labelBox.add( label );
		}

		// add the boxes to the frame
		background.add( BorderLayout.EAST, buttonBox );
		background.add( BorderLayout.WEST, labelBox );

		//	add the background panel to the frame
		frame.getContentPane().add( background );

		//	make a grid layout for the checkboxes
		grid = new GridLayout(	NUM_CHANNELS,	
								BEATS_PER_CYCLE	);

		//	customize grid layout
		grid.setVgap( 0 );
		grid.setHgap( 0 );

		centerPanel = new JPanel( grid );
		
		//	add gridpanel to the background panel
		background.add( BorderLayout.CENTER, centerPanel );

		//	create the checkboxes
		createInitialCheckboxes();

		//	add the checkboxes to the grid
		addCheckBoxesToGUI();

		//	add listeners to every GUI element
		prepGUIListeners();

		//	final frame settings
		frame.setBounds( 50, 50, WINDOW_WIDTH, WINDOW_HEIGHT );
		frame.pack();
		frame.setVisible( true );

	}

	//	create the initial checkBoxes
	public void createInitialCheckboxes()
	{
		//	fill up checkboxes with rows
		for( int i = 0; i < NUM_CHANNELS; i++ )
		{
			checkBoxes.add( new ArrayList<JCheckBox>() );
		}

		//	fill up the rows
		for( ArrayList<JCheckBox> row : checkBoxes )
		{
			for( int i = 0; i < BEATS_PER_CYCLE; i++ )
			{
				JCheckBox newCheckBox = new JCheckBox();
				newCheckBox.setSelected( false );
				row.add( newCheckBox);
			}
		}
	}

	//	add checkboxes to GUI grid layout
	public void addCheckBoxesToGUI()
	{
		for( ArrayList<JCheckBox> row : checkBoxes )
		{
			for( JCheckBox checkbox : row )
			{
				centerPanel.add( checkbox );
			}
		}
	}

	//	attack listeners to all GUI widgets
	public void prepGUIListeners()
	{
		//	start button
		buttonStart.addActionListener( new StartButtonListener() );

		//	pause button
		buttonPause.addActionListener( new PauseButtonListener() );

		//	stop button
		buttonStop.addActionListener( new StopButtonListener() );

		//	rewind button
		buttonRewind.addActionListener( new RewindButtonListener() );

		//	reset button
		buttonReset.addActionListener( new ResetButtonListener() );

		//	increase tempo button
		buttonIncreaseTempo.addActionListener( new IncreaseTempoButtonListener() );

		//	decrease tempo button
		buttonDecreaseTempo.addActionListener( new DecreaseTempoButtonListener() );

		//	every single checkbox
		addListenerToAllCheckboxes();
	}

	//	add listeners to all the checkboxes
	public void addListenerToAllCheckboxes()
	{
		for( ArrayList<JCheckBox> row : checkBoxes )
		{
			for( JCheckBox checkbox : row )
			{
				checkbox.addItemListener( this );
			}
		}
	}

	//	set all checkBoxes to off
	public void clearAllCheckboxes()
	{
		for( ArrayList<JCheckBox> row : checkBoxes )
		{
			for( JCheckBox checkBox : row )
			{
				checkBox.setSelected( false );
			}
		}
	}

	//	set all checkBoxes to off and empty the sequence
	public void clearAllCheckboxesAndClearTheSequence()
	{
		clearAllCheckboxes();
		reCreateChannelTracksForCurrentCheckboxes();
	}

	//	-	-	-	-	-	MUSIC FUNCTIONS	-	-	-	-	-	//
	//	init the midi stuff
	public void initMidi()
	{
		try
		{		
			sequencer = MidiSystem.getSequencer();
			sequencer.open();

			reCreateChannelTracksForCurrentCheckboxes();

			sequencer.setSequence( sequence );
			sequencer.setLoopCount( sequencer.LOOP_CONTINUOUSLY );
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	//	create instrument change tracks
	public void createInstrumentChangeTracks()
	{
		for( int i = 0; i < NUM_CHANNELS; i++ )
		{
			instrumentChangeTracks.add( sequence.createTrack() );
		}
	}

	//	start sequencer while preserving tempo
	public void startSequencer()
	{
		sequencer.start();
		sequencer.setTempoInBPM( beatsPerMinute );
		isPlaying = true;
	}

	//	create channel tracks
	public void createChannelTracks()
	{
			//	create channel tracks
			for( int i = 0; i < NUM_CHANNELS; i++ )
			{
				channelTracks.add( sequence.createTrack() );
			}
	}

	//	remove channel tracks
	public void removeChannelTracksFromSequenceAndEmptyChannelTracks()
	{
			if( isPlaying )
			{
				sequencer.stop();
				isPlaying = false;
			}

			try
			{
				sequence = new Sequence( Sequence.PPQ, PULSES_PER_QUARTER_NOTE );
			}
			catch( Exception ex )
			{
				ex.printStackTrace();
			}

			//	create channel tracks
			channelTracks = new ArrayList<Track>();
	}

	//	create channel tracks
	public void reCreateChannelTracksForCurrentCheckboxes()
	{
		//	remove current tracks from sequence and delete channel tracks
		removeChannelTracksFromSequenceAndEmptyChannelTracks();

		//	create channel instrument tracks
		createInstrumentChangeTracks();

		//	create new channel tracks
		createChannelTracks();

		//	fill channel instrument tracks
		initInstrumentsOfChannels();

		//	refill tracks with notes based on current checks
		makeNotesForAllCheckedBoxes();

		//	set the sequence
			try
			{
				sequencer.setSequence( sequence );
			}
			catch( Exception ex )
			{
				ex.printStackTrace();
			}

	}

	//	go through every checkbox and remake every note
	public void makeNotesForAllCheckedBoxes()
	{
			//	create channel tracks
			for( int i = 0; i < NUM_CHANNELS; i++ )
			{
				ArrayList<JCheckBox> row = checkBoxes.get( i );
				for( int j = 0; j < BEATS_PER_CYCLE; j++ )
				{
					JCheckBox checkBox = row.get( j );
					if( checkBox.isSelected() == true )
					{
						System.out.println( i + ", " + j );
						addSoundAtCheckBoxTime( i, j );
					}
				}
			}
	}


	//	delete a note when checkbox is unchecked

	//	initialize all instruments of each channel
	public void initInstrumentsOfChannels()
	{
		for( int i = 0; i < NUM_CHANNELS; i++ )
		{
			//	fetch the instrument num for that channel
			Integer instrument = instrumentOfChannel.get( i );

			//	fetch the track for that channel
			Track track = channelTracks.get( i );

			changeInstrument( i, instrument, 0, track );
		}
	}

	// change the instrument of a particular channel
	public void changeInstrument(	int channel,
									int instrument,
									int time,
									Track track 		)
	{
		try
		{
			ShortMessage instrumentChangeMessage = new ShortMessage();
			instrumentChangeMessage.setMessage( CHANGE_INSTRUMENT, channel, instrument, 0 );
			MidiEvent changeInstrumentEvent = new MidiEvent( instrumentChangeMessage, time );
			track.add( changeInstrumentEvent );
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	//	create a note and add it to the appropriate track
	public void makeNote(	int channel,
							int note,
							int time,
							int noteLength,
							Track track 		)
	{
		try
		{
			ShortMessage noteONMessage = new ShortMessage();
			noteONMessage.setMessage( NOTE_ON, channel, note, 100 );
			MidiEvent noteOnEvent = new MidiEvent( noteONMessage, time );
			track.add( noteOnEvent );

			ShortMessage noteOFFMessage = new ShortMessage();
			noteOFFMessage.setMessage( NOTE_OFF, channel, note, 0 );
			MidiEvent noteOFFEvent = new MidiEvent( noteOFFMessage, time + noteLength );
			track.add( noteOFFEvent );
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	public void addSoundAtCheckBoxTime( int channel, int time )
	{
		//	get the note currently assigned to that channel
		int note = noteOfChannel.get( channel );

		//	get the track currently assigned to that channel
		Track track = channelTracks.get( channel );

		// channel, note, time, notelength, track
		makeNote( channel, note, time, NOTE_LENGTH, track );
	}

	public MidiEvent makeEvent( int comd, int chan, int one, int two, int tick )
	{
		MidiEvent event = null;
		try
		{
			ShortMessage sm = new ShortMessage();
			sm.setMessage( comd, chan, one, two );
			event = new MidiEvent( sm, tick );
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}

		return event;
	}

	//	-	-	-	-	-	LISTENER INNER CLASSES	-	-	-	-	-	//

	public class StartButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "start button pressed" );
			if( isPlaying == false )
			{
				startSequencer();

				System.out.println( "starting" );
			}
			//	otherwise it's already playing
			//	//	so do nothing
		}
	}
	
	public class StopButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "stop button pressed" );
			//	stop the music if it is playing
			if( isPlaying == true )
			{
				sequencer.stop();
				sequencer.setTickPosition( 0 );
				isPlaying = false;

				System.out.println( "stopping" );
			}
			//	if it is not currently playing, do nothing
		}
	}
	
	public class PauseButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "pause button pressed" );
			//	pause the music if it is playing
			if( isPlaying == true )
			{
				sequencer.stop();
				isPlaying = false;

				System.out.println( "pauseing" );
			}
			//	if it is not currently playing, do nothing
		}
	}

	public class RewindButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "rewind button pressed" );
			//	start the music over from the begining
			sequencer.setTickPosition( 0 );
			System.out.println( "rewinding" );
		}
	}

	public class ResetButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "reset button pressed" );
			//	set all checkboxes to unchecked and empty the sequence
			clearAllCheckboxesAndClearTheSequence();
		}
	}

	public class IncreaseTempoButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{
			System.out.println( "increase tempo button pressed" );
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor( (float) (tempoFactor * (1.00 + TEMPO_CHANGE_FACTOR ) ) );
		}
	}

	public class DecreaseTempoButtonListener implements ActionListener
	{
		public void actionPerformed( ActionEvent ev )
		{			
			System.out.println( "decrease tempo button pressed" );
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor( (float) (tempoFactor * ( 1.00 - TEMPO_CHANGE_FACTOR ) ) );
		}
	}


	public void itemStateChanged( ItemEvent ev )
	{
		boolean wasPlaying = false;
		if( isPlaying )
		{ 
			wasPlaying  = true;
		}


		int channel = 0;
		int beatTime = 0;
		//	determine which checkbox it was
		JCheckBox sourceCheckBox = (JCheckBox) ev.getSource();
		PairInts checkBoxCoords = determineSourceCheckbox( sourceCheckBox );

		ArrayList<JCheckBox> row = checkBoxes.get( checkBoxCoords.valueOne );
		JCheckBox checkBox = row.get( checkBoxCoords.valueTwo );
		if( checkBox.isSelected() == true )
		{
			//	add note to track
			addSoundAtCheckBoxTime( checkBoxCoords.valueOne, checkBoxCoords.valueTwo );
			reCreateChannelTracksForCurrentCheckboxes();

		}
		else if( checkBox.isSelected() == false )
		{
			reCreateChannelTracksForCurrentCheckboxes();
		}

		if( wasPlaying )
		{
			startSequencer();
		}

	}

	public PairInts determineSourceCheckbox( JCheckBox sourceCheckBox )
	{
		PairInts row_checkbox = new PairInts();
		//	determine which checkbox it was
		for( ArrayList<JCheckBox> checkboxRow : checkBoxes )
		{
			if( checkboxRow.contains( sourceCheckBox ) )
			{
				row_checkbox.valueOne = checkBoxes.indexOf( checkboxRow );
				row_checkbox.valueTwo = checkboxRow.indexOf( sourceCheckBox );
				break;
			}
		}
		return row_checkbox;
	}

}
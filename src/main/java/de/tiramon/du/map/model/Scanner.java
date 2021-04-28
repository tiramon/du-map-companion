package de.tiramon.du.map.model;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.Observable;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Scanner {
	protected Logger log = LoggerFactory.getLogger(Scanner.class);

	private long id;
	private ScannerState state = ScannerState.UNKNOWN;
	private String position;
	private long timestampStarted;
	private LongProperty timeLeft = new SimpleLongProperty();
	private Timer timer;
	private ObservableList<Scan> scans = FXCollections.observableArrayList(item -> new Observable[] { item.oreMap });
	private ObjectProperty<ScannerState> stateProperty = new SimpleObjectProperty<>(ScannerState.UNKNOWN);
	private StringProperty positionProperty = new SimpleStringProperty();
	private LongProperty lastStateChangeProperty = new SimpleLongProperty();

	public Scanner(long scannerid) {
		this.id = scannerid;
	}

	public enum ScannerState {
		STARTED, FINISHED, RESULT, UNKNOWN
	}

	public void setState(ScannerState state, long timestampStateChange) {
		lastStateChangeProperty.set(timestampStateChange);
		if (state == ScannerState.STARTED) {
			timestampStarted = timestampStateChange;
			long diff = System.currentTimeMillis() - timestampStateChange;
			if (diff < 15 * 60 * 1000) {
				timeLeft.set(15 * 60 * 1000 - diff);
			} else {
				timeLeft.set(0);
			}
			if (timer != null) {
				timer.cancel();
			}
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					timeLeft.set(timeLeft.get() - 1000);

					if (timeLeft.get() <= 0)
						timer.cancel();
				}
			}, 1000, 1000);
		} else {
			timer.cancel();
		}
		this.state = state;
		this.stateProperty.set(state);
	}

	public void setTimestampStarte(long timestamp) {
		this.timestampStarted = timestamp;
	}

	public void setPosition(String position) {
		this.position = position;
		this.positionProperty.set(position);
	}

	public ScannerState getState() {
		return this.state;
	}

	public String getPosition() {
		return this.position;
	}

	public void clear() {
		scans.clear();
	}

	public ReadOnlyLongProperty idProperty() {
		return new SimpleLongProperty(id);
	}

	public ReadOnlyStringProperty positionProperty() {
		return positionProperty;
	}

	public ReadOnlyObjectProperty<ScannerState> stateProperty() {
		return stateProperty;
	}

	public ReadOnlyLongProperty timeLeftProperty() {
		return timeLeft;
	}

	public ReadOnlyLongProperty lastStateChangeProperty() {
		return lastStateChangeProperty;
	}
}

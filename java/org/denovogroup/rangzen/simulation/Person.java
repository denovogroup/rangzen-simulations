package org.denovogroup.rangzen.simulation;

import sim.engine.Steppable;
import sim.engine.SimState;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.field.network.Edge;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.DrawInfo2D;
import sim.util.Bag;

import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.awt.Color;
import java.awt.Graphics2D;

public class Person extends SimplePortrayal2D implements Steppable {
  private static final long serialVersionUID = 1;
  public int name;

  private MessagePropagationSimulation sim;

  public Map<Person, Integer> encounterCounts = new HashMap<Person, Integer>();

  /** The message queue for the node. */
  public PriorityQueue<Message> messageQueue = new PriorityQueue<Message>();

  public Person(int name, MessagePropagationSimulation sim) {
    this.name = name;
    this.sim = sim;
    
    if (name == 0) {
      messageQueue.add(new Message("0's message!", 1));
    }
  }

  public void step(SimState state) {
    MessagePropagationSimulation sim = (MessagePropagationSimulation) state;
    takeRandomStep(sim);

    // TODO(lerner): Author message with some probability.
  }
  
  private void takeRandomStep(MessagePropagationSimulation sim) {
    Double2D me = sim.space.getObjectLocation(this);
    MutableDouble2D sumForces = new MutableDouble2D();
    sumForces.addIn(new Double2D(sim.randomMultiplier * (sim.random.nextInt(5)-2 * 1.0),
          sim.randomMultiplier * (sim.random.nextInt(5)-2 * 1.0)));

    sumForces.addIn(me);
    sim.space.setObjectLocation(this, new Double2D(sumForces));

  }

  public void putMessages(PriorityQueue<Message> newMessages, Person sender) {
    Set<Object> sharedFriends = sender.findSharedFriends(this);
    for (Object friend : sharedFriends) {
      System.out.print(name+"/"+sender+": "+friend + ", ");
    }
    System.out.println();
    int otherName = sender.name;
    for (Message m : newMessages) {
      if (!messageQueue.contains(m)) {
        messageQueue.add(m);
        System.out.println(name+"/"+otherName+": "+messageQueue.peek());
      }
    }
  }

  public void encounter(Person other) {
    Integer count = encounterCounts.get(other);
    if (count == null) {
      encounterCounts.put(other, 1);
    }
    else {
      count++;
      encounterCounts.put(other, count);
    } 
    other.putMessages(messageQueue, this);
  }

  public Set<Person> getFriends() {
    Bag myEdges = sim.socialNetwork.getEdges(this, null);
    Set<Person> friends = new HashSet<Person>();

    for (Object e1 : myEdges) {
      Person from = (Person) ((Edge) e1).from();
      Person to = (Person) ((Edge) e1).to();

      if (from == this) {
        friends.add(to);
      } else {
        friends.add(from);
      }
    }
    return friends;
  }

  public Set<Object> findSharedFriends(Person other) {
    Bag myEdges = sim.socialNetwork.getEdges(this, null);
    Bag otherEdges = sim.socialNetwork.getEdges(other, null);

    Set<Object> sharedFriends = new HashSet<Object>();
    for (Object e1 : myEdges) {
      for (Object e2 : otherEdges) {
        // There has to be some way to do this more elegantly?
        Object myFrom = ((Edge) e1).from();
        Object myTo = ((Edge) e1).to();
        Object otherFrom = ((Edge) e2).from();
        Object otherTo = ((Edge) e2).to();

        Object myFriend = (myFrom == this) ? myTo : myFrom;
        Object otherFriend = (otherFrom == other) ? otherTo : otherFrom;

        System.out.println(myFrom + " " + myTo + " " + otherFrom + " " + otherTo);
        if (myFriend == otherFriend) {
          sharedFriends.add(myFriend);
        }
      }
    }
    return sharedFriends;
  }

  public String toString() {
    return "" + name;
  }

  protected Color noMessageColor = new Color(0,0,0);
  protected Color messageColor = new Color(255,0,0);
  public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
    // double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
    // double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;
    double diamx = 10;
    double diamy = 10;

    if (messageQueue.isEmpty()) {
      graphics.setColor( noMessageColor );            
    }
    else { 
      graphics.setColor( messageColor );            
    }
    graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
      
    // graphics.setColor( goodMarkColor );
    // graphics.fillRect((int)(info.draw.x-diamx/3),(int)(info.draw.y-diamy/16),(int)(diamx/1.5),(int)(diamy/8));
    // graphics.fillRect((int)(info.draw.x-diamx/16),(int)(info.draw.y-diamy/3),(int)(diamx/8),(int)(diamy/1.5));
  }
}
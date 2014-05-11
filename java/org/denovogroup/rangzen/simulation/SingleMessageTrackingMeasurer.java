package org.denovogroup.rangzen.simulation;

import sim.engine.Steppable;
import sim.engine.SimState;
import sim.util.Bag;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.TreeMap;


public class SingleMessageTrackingMeasurer implements Steppable {
  private static final long serialVersionUID = 1;

  private MessagePropagationSimulation sim;
  private Message trackedMessage;

  private int maxPropagationSeen = 0;
  private double maxTimeSeen = 0;
  private double minTimeSeen = Double.MAX_VALUE;

  // Storages the history of message propgation.
  Map<Double, Integer> timestepToPropagation;

  public SingleMessageTrackingMeasurer(MessagePropagationSimulation sim) {
    this.sim = sim;
    this.trackedMessage = new Message(UUID.randomUUID().toString(), 1.0);
    this.timestepToPropagation = new HashMap<Double, Integer>();
  }

  public void step(SimState state) {
    MessagePropagationSimulation sim = (MessagePropagationSimulation) state;
    double time = sim.schedule.getTime();
    
    if (sim.schedule.getSteps() == 0) {
      /** Compose a message */
      // authorMessage(); //Uses the first node in the network as an author
      authorMessagePopular(false);
      // System.out.println("authored message"+sim.schedule.getSteps());
    }

    Bag people = sim.socialNetwork.getAllNodes();
    int seenTrackedMessageCount = 0;
    for (int i = 0; i < people.numObjs; i++) {
      Person person = (Person) people.objs[i];
      if (person.queueHasMessageWithContent(trackedMessage)) {
        seenTrackedMessageCount++;
      }
    }
    if (seenTrackedMessageCount > maxPropagationSeen && 
        time != 0) {
      timestepToPropagation.put(time, seenTrackedMessageCount);
      maxPropagationSeen = seenTrackedMessageCount;
    }
    if (time > maxTimeSeen) {
      maxTimeSeen = time;
    }
    if (time < minTimeSeen && time != 0) {
      minTimeSeen = time;
    }

    // Stop running if the simulation has been going too long
    double hours = (time - minTimeSeen) / 1000 / 60 / 60;
    if (seenTrackedMessageCount == MessagePropagationSimulation.NUMBER_OF_PEOPLE || hours > sim.MAX_RUNTIME ) {
      sim.schedule.clear();
    }

    // System.out.println(String.format("%f: %d", time, seenTrackedMessageCount));
    // System.out.println(getMeasurementsAsJSON());
  }

  private void authorMessage() {
    Bag people = sim.socialNetwork.getAllNodes();
    // Random randomGenerator = new Random();
    if (people.numObjs > 0) {
      // Person person = (Person) people.objs[0];
      Person person = (Person) people.objs[sim.random.nextInt(people.numObjs)];
      person.addMessageToQueue(trackedMessage);
      
    }
  }
  
  private void authorMessagePopular(boolean popularFlag) {
    // if popularFlag == true, start the message from a popular node
    // else, start it from an unpopular node
    int author; 
    int boundary = 5;
    Bag people = sim.socialNetwork.getAllNodes();
    
    if (people.numObjs == 0) { 
        return;
    }
    
    // rank the social graph by degree
    List<Integer> indices = sim.orderNodesByDegree(people);
    
    //Choose an (un)popular node at random among the (bottom) top 'boundary' degrees
    author = sim.random.nextInt(boundary);
    author = 1; // either 2 or 48
    if (popularFlag) {
        author = people.numObjs - author;
    }
    int authorIdx = indices.get(author);

    // Retrieve the random 'author' element of the nodes, sorted by degree
    // This is pretty inefficient :( Must be a better way to do it
    // ArrayList<Map.Entry<Double,Object>> list = new ArrayList<Map.Entry<Double,Object>>(sorted_map.entrySet());
    // Map.Entry<Double,Object> pair = list.get(author);
    // Person person = (Person) pair.getValue();
    people = sim.socialNetwork.getAllNodes();
    Person person = (Person) people.objs[0];
    person = (Person) people.objs[authorIdx];
    // System.err.println("Degree = " + (sim.socialNetwork.getEdges(person).numObjs) + "and author is " + author);
        
    // Add the person to the queue
    person.addMessageToQueue(trackedMessage);
    
  }

  private class OutputData {
    public Map<Double, Integer> propagationData;
    public int NUMBER_OF_PEOPLE;
    public double minTimeSeen;
    public double maxTimeSeen;
    public double NEIGHBORHOOD_RADIUS;
    public double ENCOUNTER_CHANCE;
    public double priority;
  }
  public String getMeasurementsAsJSON() {
    OutputData o = new OutputData();
    o.propagationData = timestepToPropagation;
    o.minTimeSeen = minTimeSeen;
    o.maxTimeSeen = maxTimeSeen;
    o.NEIGHBORHOOD_RADIUS = ProximityEncounterModel.NEIGHBORHOOD_RADIUS;
    o.ENCOUNTER_CHANCE = ProximityEncounterModel.ENCOUNTER_CHANCE;
    o.NUMBER_OF_PEOPLE = MessagePropagationSimulation.NUMBER_OF_PEOPLE;
    o.priority = 1;

    Gson gson = new GsonBuilder().create();
    String json = gson.toJson(o);
    // System.out.println(json);
    return json;
  }

  private void encounter(Person p1, Person p2) {
    p1.encounter(p2);
    p2.encounter(p1);
      
  }
}

package org.transitime.avl;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import org.transitime.db.structs.AvlReport;
import org.transitime.modules.Module;

public class HslMqttAvlModule extends AvlModule implements MqttCallback{
    private final String broker;

    private final String topic;

    private MqttClient client;

    /**
     * Constructor
     *
     * @param agencyId
     */
    public HslMqttAvlModule(String agencyId) {
        super(agencyId);
        this.broker = "tcp://213.138.147.225:1883";
        this.topic = "/hfp/journey/#";
    }

    @Override public void run() {
        try {
            this.client = new MqttClient(this.broker, "transitime");
            this.client.setCallback(this);
            this.client.connect();
            this.client.subscribe(this.topic, 1);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override public void connectionLost(Throwable cause) {
        try {
            this.client.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
        JSONObject obj = new JSONObject(new String(message.getPayload())).getJSONObject("VP");
        AvlReport report = new AvlReport(
            obj.getString("veh"),
            obj.getLong("tsi") * 1000,
            obj.getDouble("lat"),
            obj.getDouble("long"),
            obj.isNull("spd") ? Float.NaN : (float) (double) obj.getDouble("spd"),
            obj.isNull("hdg") ? Float.NaN : (float) (double) obj.getDouble("hdg"),
            obj.isNull("source") ? "" : obj.getString("source")
        );

        String[] topicParts = topic.split("/");
        if (topicParts.length > 5 && topicParts[5] != null) {
            report.setAssignment(topicParts[5], AvlReport.AssignmentType.ROUTE_ID);
        }
        this.processAvlReport(report);
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * Just for debugging
     */
    public static void main(String[] args) {
        // Create a HslMqttAvlModule for testing
        Module.start("org.transitime.avl.HslMqttAvlModule");
    }
}

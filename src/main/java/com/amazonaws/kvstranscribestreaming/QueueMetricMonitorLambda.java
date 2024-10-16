package com.amazonaws.kvstranscribestreaming;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.ConnectClient;
import software.amazon.awssdk.services.connect.model.*;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demonstrate Amazon Connect's real-time transcription feature using AWS Kinesis Video Streams and AWS Transcribe.
 * The data flow is :
 * <p>
 * Amazon Connect => AWS KVS => AWS Transcribe => AWS DynamoDB
 *
 * <p>Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.</p>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class QueueMetricMonitorLambda implements RequestHandler<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(QueueMetricMonitorLambda.class);

    private ConnectClient connectClient = null;

    /**
     * Handler function for the Lambda
     *
     * @param object
     * @param context
     * @return
     */
    @Override
    public ConnectQueueNumber handleRequest(Object object, Context context) {

        logger.info("received request : " + object.toString());
        logger.info("received context: " + context.toString());
        logger.info("get queue info: " + object);

        String awsAccessKey = System.getenv("AWS_CONNECT_ACCESS_KEY");
        String awsSecretKey = System.getenv("AWS_CONNECT_SECRET_KEY");
        String awsRegion = System.getenv("AWS_CONNECT_REGION");
        String awsInstanceId = System.getenv("AWS_CONNECT_INSTANCE_ID");
        String awsRoutingId = System.getenv("AWS_CONNECT_ROUTING_ID");
        if(connectClient == null) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
            connectClient = ConnectClient.builder().region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
        }
        ConnectQueueNumber connectQueueNumber = new ConnectQueueNumber();
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(object);
            JsonNode rootNode = mapper.readTree(jsonString);
            String queueARN = rootNode.path("Details").path("Parameters").path("queueARN").asText();
//            int contacts =  rootNode.path("Details").path("Parameters").path("queues").asInt();
//            logger.info("contacts: " + contacts);
            CurrentMetric currentMetric = CurrentMetric.builder()
                    .name("AGENTS_ON_CONTACT")
                    .unit(Unit.COUNT)
                    .build();
            Filters filters = Filters.builder()
                    .routingProfiles(awsRoutingId)
                    .build();
            GetCurrentMetricDataRequest metricDataRequest = GetCurrentMetricDataRequest.builder()
                    .instanceId(awsInstanceId)
                    .currentMetrics(currentMetric)
                    .filters(filters)
                    .groupings(Grouping.ROUTING_PROFILE, Grouping.QUEUE)
                    .build();
            GetCurrentMetricDataResponse metricDataResponse = connectClient.getCurrentMetricData(metricDataRequest);
            List<String> queueInMaxUse = new ArrayList<>();
            if(!metricDataResponse.metricResults().isEmpty()) {
                for(CurrentMetricResult result : metricDataResponse.metricResults()) {
                    if(!result.collections().isEmpty()) {
                        for(int i = 0; i < result.collections().size(); i++) {
                            CurrentMetric metric = result.collections().get(i).metric();
                            Double value = result.collections().get(i).value();
                            //max concurrency calls per number is 5, due to each call also counted as one call before accepting phone
                            if(metric.nameAsString().equals("AGENTS_ON_CONTACT") && value >= 6d) {
                                    queueInMaxUse.add(result.dimensions().queue().arn());
                                }

                        }
                    }
                }
            }
            logger.info(queueInMaxUse.toString());
            SearchQueuesRequest queuesRequest =  SearchQueuesRequest.builder()
                    .instanceId(awsInstanceId).build();
            SearchQueuesResponse queuesResponse = connectClient.searchQueues(queuesRequest);
            List<Queue> availableQueues = !queueInMaxUse.isEmpty() ? queuesResponse.queues().stream().filter(queue -> !queueInMaxUse.contains(queue.queueArn())).collect(Collectors.toList()) : queuesResponse.queues();
            ListPhoneNumbersRequest phoneNumbersRequest = ListPhoneNumbersRequest.builder()
                    .instanceId(awsInstanceId)
                    .build();
            List<String> availablePhoneNumbers = availableQueues.stream().map(queue -> queue.outboundCallerConfig().outboundCallerIdNumberId()).collect(Collectors.toList());
            ListPhoneNumbersResponse phoneNumbersResponse = connectClient.listPhoneNumbers(phoneNumbersRequest);
            List<String> phoneNumbers = phoneNumbersResponse.phoneNumberSummaryList().stream().filter(phoneNumberSummary -> availablePhoneNumbers.contains(phoneNumberSummary.id())).map(PhoneNumberSummary::phoneNumber).collect(Collectors.toList());

            logger.info("queues: " + availableQueues.toString());
            logger.info("numbers: " + phoneNumbers.toString());
            connectQueueNumber.setOutBoundNum(phoneNumbers.get(0));
            logger.info("queue number:" + connectQueueNumber);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        return connectQueueNumber;
    }



}

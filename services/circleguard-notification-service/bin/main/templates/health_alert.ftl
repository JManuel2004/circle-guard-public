<!DOCTYPE html>
<html>
<head>
    <title>CircleGuard Health Alert</title>
</head>
<body>
    <h1>Important Health Update for ${userName!"User"}</h1>
    <p>Your health status has been updated to: <strong>${status!"UNKNOWN"}</strong></p>
    
    <#if (status!"") == "SUSPECT">
        <p>You have been in close contact with a confirmed positive case. Please follow these mandatory steps:</p>
        <ul>
            <li>Begin immediate self-isolation.</li>
            <li>Schedule a COVID-19 test: <a href='${testingUrl!"#" }'>Testing Schedule</a></li>
            <li>Review isolation guidelines: <a href='${isolationUrl!"#" }'>Guidelines</a></li>
        </ul>
    <#elseif (status!"") == "PROBABLE">
        <p>You have been in a high-risk area. We recommend the following precautions:</p>
        <ul>
            <li>Monitor for symptoms daily.</li>
            <li>Maintain strict physical distancing.</li>
            <li>Work/Study remotely if possible.</li>
        </ul>
    <#else>
        <p>Please check the CircleGuard app for more details regarding your status update.</p>
    </#if>
    
    <p>Stay safe,<br/>CircleGuard Team</p>
</body>
</html>

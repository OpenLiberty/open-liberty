<html>
<head><title>Roll a dice</title></head>
<body>
  <%
    java.util.Random rand = new java.util.Random();
  %>
  <h2>Roll a dice....</h2>(<%= rand.nextInt(5) + 1 %>)
</body>
</html>
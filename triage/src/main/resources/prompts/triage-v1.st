You are triaging a temperature excursion on a cold-chain shipment for the people who carry it and
receive it. Name the most probable cause, how serious it is, and what to do next. Your answer is
shown to them as advice next to the measurements, never as a finding, so when the readings do not
point at one cause, answer UNKNOWN with a low confidence rather than guess.

The excursion
- Kind: {kind}
- Opened at: {openedAt}
- Closed at: {closedAt}
- Minutes out of range: {durationMinutes}
- Peak reading: {peak}

The thresholds frozen when the shipment was dispatched
{thresholds}

Custody
- Held when the excursion opened by the {custodian}
- Minutes since the last accepted handoff: {minutesSinceLastHandoff}
- Handoffs inside the readings window:
{handoffs}

Readings from thirty minutes before the excursion opened to thirty minutes after it closed, in
degrees Celsius, oldest first:
{readings}

How each cause usually looks in the readings
- REFRIGERATION_FAILURE: a slow, steady drift out of range that keeps going until someone acts.
- DOOR_OPENING: a short, sharp spike that recovers on its own within minutes.
- HANDLING_DURING_HANDOFF: a jump that starts within minutes of custody changing hands.
- SENSOR_FAULT: readings that stay identical for too long, jumps no real load could make, or a gap
  in the readings followed by a jump.
- UNKNOWN: none of these fits well enough to tell the holder something useful.

Severity
- HIGH when the excursion lasted at least the single-excursion allowance, MEDIUM from half of it,
  LOW below that. Raise it one level when the peak lies more than 5 °C beyond the limit it crossed.

What to write
- confidence: your probability, between 0 and 1, that the cause you named is the right one.
- summary: two or three plain sentences saying what happened and what points at the cause, written
  for the people handling the shipment, with the same content in English (en), Spanish (es) and
  Portuguese (pt), each under 400 characters.
- recommendedAction: one instruction in English for whoever holds the shipment now, under 300
  characters.

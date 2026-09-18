package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ProblemDetails;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ResponseFactory;
import com.coldchain.shared.response.SuccessOutcome;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HttpContractArchTest {

    private static final Path SHARED = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "shared"));

    private static final Path DELIVERY = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "delivery"));

    private static final Path MODULES = SourceTree.MAIN.resolve(Path.of("com", "coldchain", "modules"));

    private static final List<String> ENVELOPE_FIELDS =
            List.of("status", "code", "messageKey", "message", "data", "traceId", "timestamp", "meta");

    private static final List<String> PROBLEM_EXTENSIONS =
            List.of("errorCode", "messageKey", "category", "timestamp", "traceId");

    private static final List<String> HTTP_VOCABULARY =
            List.of("HttpStatus", "ResponseEntity", "@ResponseStatus");

    @Test
    void theSuccessEnvelopeCarriesItsEightFields() {
        List<String> components = Arrays.stream(ApiResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(components)
                .describedAs("a typed client is written against these eight and nothing else")
                .containsExactlyElementsOf(ENVELOPE_FIELDS);
    }

    @Test
    void theErrorShapeIsProblemJsonPlusItsExtensions() {
        String problemDetails = SourceTree.read(SourceTree.find("ProblemDetails", SourceTree.MAIN).orElseThrow());

        assertThat(problemDetails)
                .describedAs("RFC 9457 gives five members; the contract adds five of its own")
                .contains("ProblemDetail.forStatus")
                .contains("problem.setType")
                .contains("problem.setTitle")
                .contains("problem.setDetail")
                .contains("problem.setInstance");
        assertThat(PROBLEM_EXTENSIONS).allSatisfy(extension -> assertThat(problemDetails)
                .describedAs("the problem body must carry %s", extension)
                .contains("setProperty(\"" + extension + "\""));
    }

    @Test
    void thereIsNoSuccessFlagAndOnlyOneAsymmetry() {
        List<String> components = Arrays.stream(ApiResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(components)
                .describedAs("the status already says whether it went well")
                .doesNotContain("success", "ok", "error");
        assertThat(components)
                .describedAs("the envelope says message; the problem says detail, and RFC 9457 fixes that name")
                .contains("message")
                .doesNotContain("detail");
    }

    @Test
    void everyEnvelopeFieldIsAlwaysSerialised() {
        assertThat(SourceTree.read(Path.of("src", "main", "resources", "application.yml")))
                .describedAs("a client cannot depend on a field being present, so null is serialised too")
                .contains("default-property-inclusion: always");
        assertThat(SourceTree.read(SourceTree.find("ApiResponse", SourceTree.MAIN).orElseThrow()))
                .describedAs("no field may opt out of being serialised")
                .doesNotContain("@JsonInclude");
    }

    @Test
    void theDomainNeverNamesAnHttpStatus() {
        List<String> speakingHttp = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> HTTP_VOCABULARY.stream()
                        .anyMatch(word -> SourceTree.read(source).contains(word)))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(speakingHttp)
                .describedAs("a module chooses the meaning of a failure; the edge chooses its status")
                .isEmpty();

        List<Path> errorCodes = SourceTree.javaFiles(MODULES).stream()
                .filter(source -> source.getFileName().toString().endsWith("ErrorCode.java"))
                .toList();

        assertThat(errorCodes).describedAs("a module names its failures in its own enum").isNotEmpty();
        assertThat(errorCodes).allSatisfy(source -> assertThat(SourceTree.read(source))
                .describedAs("%s must map every failure to a category, never to a status", source)
                .contains("ErrorCategory."));
    }

    @Test
    void theHttpStatusOfAnErrorComesFromItsCategory() {
        assertThat(Arrays.stream(ErrorCategory.values()).toList())
                .describedAs("every category decides a status, so none can fall through to 500 by accident")
                .allSatisfy(category -> assertThat(ProblemDetails.statusOf(category)).isNotNull());

        assertThat(ProblemDetails.statusOf(ErrorCategory.BUSINESS_RULE))
                .describedAs("a rejected business rule is not an infrastructure failure")
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(ProblemDetails.statusOf(ErrorCategory.CONFLICT))
                .describedAs("losing a race is an expected outcome, not a broken server")
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ProblemDetails.statusOf(ErrorCategory.RATE_LIMIT))
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void oneAdviceTranslatesEveryException() {
        List<String> advices = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> SourceTree.read(source).contains("@RestControllerAdvice")
                        || SourceTree.read(source).contains("@ControllerAdvice"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(advices)
                .describedAs("a second translator is a second contract nobody compares with the first")
                .containsExactly("GlobalExceptionHandler.java");
    }

    @Test
    void noControllerAssemblesItsOwnResponse() {
        List<String> handMade = controllers().stream()
                .filter(source -> {
                    String text = SourceTree.read(source);
                    return text.contains("ResponseEntity.ok") || text.contains("ResponseEntity.status")
                            || text.contains("new ApiResponse") || text.contains("ObjectMapper");
                })
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(controllers()).describedAs("there are controllers to check").isNotEmpty();
        assertThat(handMade)
                .describedAs("a controller asks the factory for the response; it does not build one")
                .isEmpty();
    }

    @Test
    void noControllerChoosesAnHttpStatusItself() {
        List<String> choosing = controllers().stream()
                .filter(source -> {
                    String text = SourceTree.read(source);
                    return text.contains("HttpStatus.") || text.contains("@ResponseStatus");
                })
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(choosing)
                .describedAs("the success code carries the status, the same way the category carries it "
                        + "for an error")
                .isEmpty();

        assertThat(Arrays.stream(SuccessOutcome.values()).toList())
                .describedAs("every outcome decides a status")
                .allSatisfy(outcome -> assertThat(ResponseFactory.statusOf(outcome)).isNotNull());
        assertThat(ResponseFactory.statusOf(SuccessOutcome.CREATED)).isEqualTo(HttpStatus.CREATED);
        assertThat(ResponseFactory.statusOf(SuccessOutcome.ACCEPTED)).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void theProblemTypeIsDerivedAndNeverWritten() {
        String problemType = SourceTree.read(SourceTree.find("ProblemType", SourceTree.MAIN).orElseThrow());

        assertThat(problemType)
                .describedAs("the type is built from the message key, so it cannot drift from the code")
                .contains("messageKey.substring")
                .contains("URI.create(BASE + identifier)");

        List<String> writtenByHand = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(source -> !source.getFileName().toString().equals("ProblemType.java"))
                .filter(source -> SourceTree.read(source).contains("urn:coldchain:problem"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(writtenByHand)
                .describedAs("a type somebody typed is a URL that drifts the day the key changes")
                .isEmpty();
    }

    @Test
    void validationAndRateLimitCarryTheirOwnExtensions() {
        assertThat(SourceTree.read(SourceTree.find("ProblemDetails", SourceTree.MAIN).orElseThrow()))
                .describedAs("a validation failure says which fields and why")
                .contains("setProperty(\"errors\", fieldErrors)");
        assertThat(SourceTree.read(SourceTree.find("RateLimitFilter", SourceTree.MAIN).orElseThrow()))
                .describedAs("a throttled caller is told when to come back, in the body and in the header")
                .contains("Map.of(\"retryAfter\", retryAfter)")
                .contains("HttpHeaders.RETRY_AFTER");
    }

    @Test
    void paginationMetadataComesFromTheFactory() {
        List<String> buildingTheirOwn = controllers().stream()
                .filter(source -> SourceTree.read(source).contains("PaginationMeta"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(buildingTheirOwn)
                .describedAs("every paginating endpoint has the same shape because one place fills it")
                .isEmpty();

        List<String> paginating = controllers().stream()
                .filter(source -> SourceTree.read(source).contains("Pageable"))
                .map(source -> source.getFileName().toString())
                .toList();

        assertThat(paginating).describedAs("there is a paginating endpoint to check").isNotEmpty();
        assertThat(paginating).allSatisfy(name -> assertThat(controllers().stream()
                .filter(source -> source.getFileName().toString().equals(name))
                .map(SourceTree::read)
                .findFirst()
                .orElseThrow())
                .describedAs("%s takes a Pageable, so it must answer through paginated(...)", name)
                .contains(".paginated("));
    }

    @Test
    void bothShapesCarryTheSameTraceId() {
        assertThat(SourceTree.read(SourceTree.find("ResponseFactory", SourceTree.MAIN).orElseThrow()))
                .describedAs("the success envelope carries the request identifier")
                .contains("RequestTrace.current()");
        assertThat(SourceTree.read(SourceTree.find("ProblemDetails", SourceTree.MAIN).orElseThrow()))
                .describedAs("the problem body carries the same one, under the same name")
                .contains("setProperty(\"traceId\", RequestTrace.current())");
    }

    @Test
    void everyEndpointIsDocumentedInOpenApi() {
        assertThat(controllers()).isNotEmpty();
        assertThat(controllers()).allSatisfy(source -> {
            String text = SourceTree.read(source);
            long handlers = text.lines()
                    .filter(line -> line.trim().startsWith("@GetMapping")
                            || line.trim().startsWith("@PostMapping")
                            || line.trim().startsWith("@PutMapping")
                            || line.trim().startsWith("@PatchMapping")
                            || line.trim().startsWith("@DeleteMapping"))
                    .count();
            long documented = text.lines().filter(line -> line.trim().startsWith("@Operation(")).count();

            assertThat(text)
                    .describedAs("%s must group its endpoints under a tag", source.getFileName())
                    .contains("@Tag(");
            assertThat(documented)
                    .describedAs("%s documents %d of its %d endpoints", source.getFileName(),
                            documented, handlers)
                    .isEqualTo(handlers);
            assertThat(text)
                    .describedAs("%s must describe itself in annotations, never in javadoc",
                            source.getFileName())
                    .doesNotContain("/**");
        });
    }

    private List<Path> controllers() {
        return SourceTree.javaFiles(DELIVERY).stream()
                .filter(source -> SourceTree.read(source).contains("@RestController"))
                .toList();
    }
}

$("#btnSubmit").click(function () {
    if ($("#indexname").val().length === 0) {
        showAlert(" Please specify an index.");
        return;
    }

    if ($("#partitions").val().length === 0) {
        showAlert(" Please specify the number of partitions.");
        return;
    }


    $("#btnSubmit").addClass('disabled');
    $("#btnSubmit").prop('disabled', true);

    $("#progress").show();

    var result = {};
    result.indexname = $("#indexname").val();
    result.partitions = $("#partitions").val();
    result.columns = $("#columns").val().split(",");
    result.materialize = $('#materialize').is(':checked');
    result.replace = $('#replace').is(':checked');
    result.usemetadata = $('#metadata').is(':checked');


    $.ajax("/index/repartition", {
        data: JSON.stringify(result),
        contentType: 'application/json',
        type: 'POST',
        success: function (data) {
            if (data.code === 200) {
                showAlert("index repartitioned to " + data.message);
            } else {
                showAlert("Error in request: " + data.message);
            }
            $("#progress").hide()
            $("#btnSubmit").removeClass('disabled');
            $("#btnSubmit").prop('disabled', false);
        },
        error : function() {
            $("#progress").hide()
            $("#btnSubmit").removeClass('disabled');
            $("#btnSubmit").prop('disabled', false);
            showAlert("Unspecified error in request.");
        }
    });
});
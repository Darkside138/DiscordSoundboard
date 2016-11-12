$(document).ready(function() {
    $.ajax({
        url: "/soundsApi/availableSounds",
        success: function( data ) {
            $(".buttonContainer").empty();
            $.each(data, function(i, obj) {
                var buttonContainerSelector = $(".buttonContainer");
                //use obj.id and obj.name here, for example:
                buttonContainerSelector.append($("<button></button>")
                    .attr("class", 'soundButton')
                    .attr("id", obj.soundFileId)
                    .attr("value", obj.soundFileId)
                    .attr("data-category", obj.category)
                    .text(obj.soundFileId));
                buttonContainerSelector.append("<div class='divider'/>");
            });
            $("button, input:button").button();

            $(".soundButton").click(function() {
                var volume = $('#volume').slider("option", "value");
                $.ajax({
                    url: "/soundsApi/volume?volume=" + volume,
                    method: 'POST'
                });

                var username = $(".userNameSelect option:selected").text();
                $.ajax({
                    url: "/soundsApi/playFile?soundFileId=" + this.value + "&username=" + username,
                    method: 'POST'
                });
            });
        }
    });

    $("#randomButton").click(function() {
        var username = $(".userNameSelect option:selected").text();
        $.ajax({
            url: "/soundsApi/playRandom?username=" + username,
            method: 'POST'
        });
    });

    $.ajax({
        url: "/soundsApi/users",
        success: function(data) {
            $.each(data, function(i, obj) {
                $('.userNameSelect')
                    .append($("<option></option>")
                        .attr("value",obj.username)
                        .text(obj.username));
                if (obj.selected) {
                    $(".userNameSelect option[value='" + obj.username + "']").attr('selected', 'selected');
                }
            });
            $('.userNameSelect').selectmenu({ width : 'auto'});
        }
    });

    $.ajax({
        url: "/soundsApi/soundCategories",
        success: function(data) {
            if (data.length > 1) {
                $.each(data, function(i, obj) {
                    $('.categorySelect')
                        .append($("<option></option>")
                            .attr("value",obj)
                            .text(obj));
                });
                $('.categorySelect').selectmenu({
                    width : 'auto',
                    change: function( event, ui ) {
                        var soundButtonSelector = $('.soundButton');
                        if (ui.item.value) {
                            var matching = soundButtonSelector.filter(function(){
                                return $(this).attr('data-category') == ui.item.value
                            });
                            soundButtonSelector.hide();
                            matching.prop('selected', true).show();
                        } else {
                            soundButtonSelector.show();
                        }

                    }
                });
                $('.categoryContainer').show();
            } else {
                $('.categoryContainer').hide();
            }
        }
    });

    $(".randomButton").click(function() {
        var volume = $('#volume').slider("option", "value");
        $.ajax({
            url: "/soundsApi/volume?volume=" + volume,
            method: 'POST'
        });

        var username = $(".userNameSelect option:selected").text();
        $.ajax({
            url: "/soundsApi/playRandom?username=" + username,
            method: 'POST'
        });
    });

    $("#volume").slider({
        min: 0,
        max: 100,
        value: 75,
        range: "min",
        animate: true,
        slide: function(event, ui) {
            var volume = ui.value;
            $.ajax({
                url: "/soundsApi/volume?volume=" + volume,
                method: 'POST'
            });
        }
    });
});
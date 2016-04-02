$(document).ready(function() {
    $.ajax({
        url: "/soundsApi/getAvailableSounds",
        success: function( data ) {
            $(".buttonContainer").empty();
            $.each(data, function(i, obj) {
                //use obj.id and obj.name here, for example:
                $(".buttonContainer").append($("<button></button>")
                    .attr("class", 'soundButton')
                    .attr("id", obj.soundFileId)
                    .attr("value", obj.soundFileId)
                    .attr("data-category", obj.category)
                    .text(obj.soundFileId));
                $(".buttonContainer").append("<div class='divider'/>");
            });
            $("button, input:button").button();

            $(".soundButton").click(function() {
                var volume = $('#volume').slider("option", "value");
                $.ajax({
                    url: "/soundsApi/setVolume?volume=" + volume,
                    method: 'POST'
                });

                var username = $(".userNameSelect option:selected").text();
                $.ajax({
                    url: "/soundsApi/playFile?soundFileId=" + this.value + "&username=" + username
                });
            });
        }
    });

    $.ajax({
        url: "/soundsApi/getUsers",
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
        url: "/soundsApi/getSoundCategories",
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
                        if (ui.item.value) {
                            var matching = $('.soundButton').filter(function(){
                                return $(this).attr('data-category') == ui.item.value
                            });
                            $('.soundButton').hide();
                            matching.prop('selected', true).show();
                        } else {
                            $('.soundButton').show();
                        }

                    }
                });
                $('.categoryContainer').show();
            } else {
                $('.categoryContainer').hide();
            }
        }
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
                url: "/soundsApi/setVolume?volume=" + volume,
                method: 'POST'
            });
        }
    });
});
(function($, window){
    var arrowWidth = 30;

    $.fn.resizeselect = function(settings) {
        return this.each(function() {

            $(this).change(function(){
                var $this = $(this);

                // create test element
                var text = $this.find("option:selected").text();
                var $test = $("<span>").html(text);

                // add to body, get width, and get out
                $test.appendTo('body');
                var width = $test.width();
                $test.remove();

                // set select width
                $this.width(width + arrowWidth);

                // run on start
            }).change();

        });
    };

    // run by default
    $("select.resizeselect").resizeselect();

})(jQuery, window);
/*
 * Jquery plugin code based on: http://www.barneyb.com/barneyblog/2008/01/08/checkbox-range-selection-a-la-gmail/ (with enhancements by "Anne", in the comments section)
 */
(function ($) {
    $.fn.enableCheckboxRangeSelection = function () {
        var lastCheckbox = null;
        var $spec = this;
        $spec.unbind("click.checkboxrange");
        $spec.bind("click.checkboxrange", function (e) {
            var alreadyChecked = true;
            var currentTarget = e.target;
            if (typeof currentTarget.htmlFor !== 'undefined')
            {
                currentTarget = document.getElementById(currentTarget.htmlFor);
                alreadyChecked = false;
            }
            if (lastCheckbox != null && (e.shiftKey || e.metaKey)) {
                var elements = $spec.slice(
                Math.min($spec.index(lastCheckbox), $spec.index(currentTarget)),
                Math.max($spec.index(lastCheckbox), $spec.index(currentTarget)) + 1
                );
                if (currentTarget.checked == alreadyChecked){
                    elements.attr({ checked:"checked"});
                }
                else{
                    elements.removeAttr("checked");
                }
            }
            lastCheckbox = currentTarget;

            //Hack: Reset the value of the input when a label is clicked and then trigger the click of the input self. Now this solution is compatible with events on that input (like a custom change event)

            if (!alreadyChecked && (e.shiftKey || e.metaKey)){
                e.preventDefault();
                if (currentTarget.checked == true){
                    $(currentTarget).removeAttr("checked");
                }
                else{
                    $(currentTarget).attr({ checked:"checked"});
                }
                currentTarget.click();
            }

            return true;
        });
    };
})(jQuery);

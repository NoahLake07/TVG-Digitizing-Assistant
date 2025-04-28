import React from 'react';

function ConversionPanel({ onPreviousConversion, onNextConversion, ...props }) {
    function handleKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
        if (e.key === '[') {
            onPreviousConversion?.();
            e.preventDefault();
        } else if (e.key === ']') {
            onNextConversion?.();
            e.preventDefault();
        }
    }

    return (
        <div
            tabIndex={0}
            onKeyDown={handleKeyDown}
            style={{ outline: 'none' }} // Optional: remove focus ring
            // ...other props
        >
            {/* ...existing panel content... */}
        </div>
    );
}

export default ConversionPanel; 
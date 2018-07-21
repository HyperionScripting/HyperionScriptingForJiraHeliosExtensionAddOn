module Helios {
    export interface HeliosAccessor {
        readonly SystemManager: SystemManager;
    }

    /**
    *   Contains system related functions.
    */
    export interface SystemManager {
        /**
        *   Returns max heap size.
        */
        getMaxHeapSize(): number;
    }
}